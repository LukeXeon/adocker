package com.github.andock.ksp

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

class RouteProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Find all symbols annotated with @Route
        val routeAnnotatedSymbols = resolver
            .getSymbolsWithAnnotation("com.github.andock.ui.route.Route")
            .filterIsInstance<KSFunctionDeclaration>()

        routeAnnotatedSymbols.forEach { function ->
            // Validate function has @Composable annotation
            if (!validateFunction(function)) {
                return@forEach
            }

            // Extract route data from annotations
            val routeData = extractRouteData(function) ?: return@forEach

            // Generate Hilt module
            generateHiltModule(function, routeData)
        }

        return emptyList()
    }

    /**
     * Validates that the function meets requirements for @Route annotation
     */
    private fun validateFunction(function: KSFunctionDeclaration): Boolean {
        var isValid = true

        // Check 1: Must have @Composable annotation
        val hasComposable = function.annotations.any {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == "androidx.compose.runtime.Composable"
        }
        if (!hasComposable) {
            logger.error(
                "@Route annotation can only be used on @Composable functions. " +
                        "Add @Composable annotation to ${function.simpleName.asString()}",
                symbol = function
            )
            isValid = false
        }

        // Check 2: Must be top-level function or inside an object
        val parent = function.parentDeclaration
        val isTopLevel = parent == null || parent !is KSClassDeclaration
        val isInObject = parent is KSClassDeclaration &&
                         parent.classKind == ClassKind.OBJECT

        if (!isTopLevel && !isInObject) {
            logger.error(
                "@Route annotation can only be used on top-level functions or functions inside an object. " +
                        "Function ${function.simpleName.asString()} is inside ${parent.simpleName.asString()} which is not an object.",
                symbol = function
            )
            isValid = false
        }

        // Check 3: Warn if function has parameters
        if (function.parameters.isNotEmpty()) {
            logger.warn(
                "@Route function ${function.simpleName.asString()} has parameters. " +
                        "Ensure it can be invoked without arguments in the generated Screen lambda.",
                symbol = function
            )
        }

        return isValid
    }

    /**
     * Extracts route data from @Route annotation
     */
    private fun extractRouteData(function: KSFunctionDeclaration): RouteData? {
        val routeAnnotation = function.annotations.firstOrNull {
            it.shortName.asString() == "Route"
        } ?: return null

        // Extract route type (KClass<*>)
        val routeTypeArg = routeAnnotation.arguments
            .firstOrNull { it.name?.asString() == "type" }

        if (routeTypeArg == null) {
            logger.error(
                "@Route annotation must specify a 'type' parameter",
                symbol = function
            )
            return null
        }

        val routeType = routeTypeArg.value as? KSType
        if (routeType == null) {
            logger.error(
                "@Route annotation 'type' parameter must be a valid class reference",
                symbol = function
            )
            return null
        }

        // Extract deepLinks array
        val deepLinksArg = routeAnnotation.arguments
            .firstOrNull { it.name?.asString() == "deepLinks" }
            ?.value as? ArrayList<*> ?: emptyList<Any>()

        val deepLinks = deepLinksArg.mapNotNull { deepLinkAnnotation ->
            if (deepLinkAnnotation is KSAnnotation) {
                extractDeepLinkData(deepLinkAnnotation)
            } else {
                null
            }
        }

        return RouteData(
            functionName = function.simpleName.asString(),
            packageName = function.packageName.asString(),
            routeTypeClassName = routeType.toClassName(),
            deepLinks = deepLinks
        )
    }

    /**
     * Extracts data from @DeepLink annotation
     */
    private fun extractDeepLinkData(annotation: KSAnnotation): DeepLinkData {
        val uriPattern = annotation.arguments
            .firstOrNull { it.name?.asString() == "uriPattern" }
            ?.value as? String ?: ""

        val action = annotation.arguments
            .firstOrNull { it.name?.asString() == "action" }
            ?.value as? String ?: ""

        val mimeType = annotation.arguments
            .firstOrNull { it.name?.asString() == "mimeType" }
            ?.value as? String ?: ""

        return DeepLinkData(uriPattern, action, mimeType)
    }

    /**
     * Generates Hilt module for the route
     */
    private fun generateHiltModule(
        function: KSFunctionDeclaration,
        routeData: RouteData
    ) {
        val moduleName = "${routeData.functionName}Module"

        // Generate in function's package + .generated
        val generatedPackageName = "${routeData.packageName}.generated"

        val fileSpec = FileSpec.builder(
            packageName = generatedPackageName,
            fileName = moduleName
        ).apply {
            // Add import for the composable function
            addImport(routeData.packageName, routeData.functionName)
            addType(buildModuleType(routeData, moduleName))
        }.build()

        // Write to file
        try {
            fileSpec.writeTo(
                codeGenerator = codeGenerator,
                dependencies = Dependencies(false, function.containingFile!!)
            )
            logger.info("Generated Hilt module: $moduleName for function ${routeData.functionName}")
        } catch (e: Exception) {
            logger.error(
                "Failed to generate Hilt module for ${routeData.functionName}: ${e.message}",
                symbol = function
            )
        }
    }

    /**
     * Builds the module TypeSpec
     */
    private fun buildModuleType(routeData: RouteData, moduleName: String): TypeSpec {
        return TypeSpec.objectBuilder(moduleName)
            .addAnnotation(ClassName("dagger", "Module"))
            .addAnnotation(
                AnnotationSpec.builder(ClassName("dagger.hilt", "InstallIn"))
                    .addMember(
                        "%T::class",
                        ClassName("dagger.hilt.components", "SingletonComponent")
                    )
                    .build()
            )
            .addFunction(buildScreenProviderFunction(routeData))
            .build()
    }

    /**
     * Builds the screen provider function
     */
    private fun buildScreenProviderFunction(routeData: RouteData): FunSpec {
        val screenClassName = ClassName("com.github.andock.ui.route", "Screen")

        return FunSpec.builder("screen")
            .addAnnotation(ClassName("dagger", "Provides"))
            .addAnnotation(ClassName("dagger.multibindings", "IntoMap"))
            .addAnnotation(
                AnnotationSpec.builder(ClassName("dagger.multibindings", "ClassKey"))
                    .addMember("%T::class", routeData.routeTypeClassName)
                    .build()
            )
            .returns(screenClassName)
            .apply {
                if (routeData.deepLinks.isEmpty()) {
                    // Simple case: no deep links
                    addStatement(
                        "return %T { %N() }",
                        screenClassName,
                        routeData.functionName
                    )
                } else {
                    // With deep links
                    addCode(buildScreenWithDeepLinks(routeData))
                }
            }
            .build()
    }

    /**
     * Builds Screen constructor with deep links
     */
    private fun buildScreenWithDeepLinks(routeData: RouteData): CodeBlock {
        val screenClassName = ClassName("com.github.andock.ui.route", "Screen")
        val navDeepLinkFunction = MemberName("androidx.navigation", "navDeepLink")

        return buildCodeBlock {
            addStatement("return %T(", screenClassName)
            indent()
            addStatement("deepLinks = listOf(")
            indent()

            routeData.deepLinks.forEachIndexed { index, deepLink ->
                if (index > 0) {
                    add(",\n")
                }

                // Build navDeepLink DSL
                add("%M {\n", navDeepLinkFunction)
                indent()

                // Only add non-empty properties
                if (deepLink.uriPattern.isNotEmpty()) {
                    addStatement("uriPattern = %S", deepLink.uriPattern)
                }
                if (deepLink.action.isNotEmpty()) {
                    addStatement("action = %S", deepLink.action)
                }
                if (deepLink.mimeType.isNotEmpty()) {
                    addStatement("mimeType = %S", deepLink.mimeType)
                }

                unindent()
                add("}")
            }

            add("\n")
            unindent()
            addStatement(")") // Close listOf
            unindent()
            add(") {\n") // Close Screen constructor, start lambda
            indent()
            addStatement("%N()", routeData.functionName)
            unindent()
            add("}\n")
        }
    }

    /**
     * Data class to hold route information
     */
    private data class RouteData(
        val functionName: String,
        val packageName: String,
        val routeTypeClassName: ClassName,
        val deepLinks: List<DeepLinkData>
    )

    /**
     * Data class to hold deep link information
     */
    private data class DeepLinkData(
        val uriPattern: String,
        val action: String,
        val mimeType: String
    )

    @Suppress("unused")
    @AutoService(SymbolProcessorProvider::class)
    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return RouteProcessor(
                codeGenerator = environment.codeGenerator,
                logger = environment.logger
            )
        }
    }
}
