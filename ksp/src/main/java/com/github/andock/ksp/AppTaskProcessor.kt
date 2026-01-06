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
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class AppTaskProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Find all symbols annotated with @AppTask
        val appTaskAnnotatedSymbols = resolver
            .getSymbolsWithAnnotation("com.github.andock.daemon.app.AppTask")
            .filterIsInstance<KSFunctionDeclaration>()

        appTaskAnnotatedSymbols.forEach { function ->
            // Validate function meets requirements for @AppTask annotation
            if (!validateFunction(function)) {
                return@forEach
            }

            // Extract task data from annotations
            val taskData = extractTaskData(function) ?: return@forEach

            // Generate Hilt module
            generateHiltModule(function, taskData)
        }

        return emptyList()
    }

    /**
     * Validates that the function meets requirements for @AppTask annotation
     */
    private fun validateFunction(function: KSFunctionDeclaration): Boolean {
        var isValid = true

        // Check: Must be top-level function or inside an object
        val parent = function.parentDeclaration
        val isTopLevel = parent == null || parent !is KSClassDeclaration
        val isInObject = parent is KSClassDeclaration &&
                parent.classKind == ClassKind.OBJECT

        if (!isTopLevel && !isInObject) {
            logger.error(
                "@AppTask annotation can only be used on top-level functions or functions inside an object. " +
                        "Function ${function.simpleName.asString()} is inside ${parent.simpleName.asString()} which is not an object.",
                symbol = function
            )
            isValid = false
        }

        return isValid
    }

    /**
     * Extracts task data from @AppTask annotation
     */
    private fun extractTaskData(function: KSFunctionDeclaration): TaskData? {
        val appTaskAnnotation = function.annotations.firstOrNull {
            it.shortName.asString() == "AppTask"
        } ?: return null

        // Extract task name
        val taskNameArg = appTaskAnnotation.arguments
            .firstOrNull { it.name?.asString() == "name" }

        if (taskNameArg == null) {
            logger.error(
                "@AppTask annotation must specify a 'name' parameter",
                symbol = function
            )
            return null
        }

        val taskName = taskNameArg.value as? String
        if (taskName == null) {
            logger.error(
                "@AppTask annotation 'name' parameter must be a valid string",
                symbol = function
            )
            return null
        }

        // Extract return type
        val returnType = function.returnType?.resolve()?.toTypeName() ?: run {
            logger.error(
                "@AppTask function ${function.simpleName.asString()} must have a return type",
                symbol = function
            )
            return null
        }

        // Extract parameters
        val parameters = function.parameters.map { param ->
            extractParameterData(param)
        }

        return TaskData(
            functionName = function.simpleName.asString(),
            packageName = function.packageName.asString(),
            taskName = taskName,
            returnType = returnType,
            parameters = parameters
        )
    }

    /**
     * Extracts parameter data including @AppTask annotation if present
     */
    private fun extractParameterData(parameter: KSValueParameter): ParameterData {
        // Check if parameter has @AppTask annotation
        val appTaskAnnotation = parameter.annotations.firstOrNull {
            it.shortName.asString() == "AppTask"
        }

        val hasAppTask = appTaskAnnotation != null
        val appTaskName = if (hasAppTask) {
            appTaskAnnotation.arguments
                .firstOrNull { it.name?.asString() == "name" }
                ?.value as? String
        } else null

        // Collect other annotations (excluding @AppTask)
        val otherAnnotations = parameter.annotations
            .filter { it.shortName.asString() != "AppTask" }
            .map { convertToAnnotationSpec(it) }
            .toList()

        // Get parameter type preserving all annotations including type argument annotations
        val resolvedType = parameter.type.resolve()
        val baseType = resolvedType.toTypeName()

        // Try to preserve type argument annotations for parameterized types
        val paramType = if (baseType is ParameterizedTypeName && resolvedType.arguments.isNotEmpty()) {
            // Reconstruct the parameterized type with type argument annotations
            val typeArgs = resolvedType.arguments.mapIndexed { index, typeArg ->
                val argType = typeArg.type?.resolve()
                if (argType != null) {
                    val argTypeName = argType.toTypeName()
                    // Check for annotations on the type argument
                    val argAnnotations = typeArg.type?.annotations
                        ?.map { convertToAnnotationSpec(it) }
                        ?.toList() ?: emptyList()

                    if (argAnnotations.isNotEmpty()) {
                        argTypeName.copy(annotations = argAnnotations)
                    } else {
                        argTypeName
                    }
                } else {
                    baseType.typeArguments[index]
                }
            }

            if (typeArgs.isNotEmpty()) {
                baseType.rawType.parameterizedBy(typeArgs)
            } else {
                baseType
            }
        } else {
            baseType
        }

        return ParameterData(
            name = parameter.name?.asString() ?: "",
            type = paramType,
            hasAppTask = hasAppTask,
            appTaskName = appTaskName,
            annotations = otherAnnotations
        )
    }

    /**
     * Converts KSAnnotation to AnnotationSpec
     * Note: We skip complex annotations to avoid Kotlin compiler issues
     */
    private fun convertToAnnotationSpec(annotation: KSAnnotation): AnnotationSpec {
        val annotationType = annotation.annotationType.resolve().toClassName()
        val builder = AnnotationSpec.builder(annotationType)

        // Add annotation arguments
        annotation.arguments.forEach { arg ->
            val argName = arg.name?.asString()
            val argValue = arg.value
            if (argName != null && argValue != null) {
                when (argValue) {
                    is String -> builder.addMember("%S", argValue)
                    is Int -> builder.addMember("%L", argValue)
                    is Long -> builder.addMember("%LL", argValue)
                    is Float -> builder.addMember("%LF", argValue)
                    is Double -> builder.addMember("%L", argValue)
                    is Boolean -> builder.addMember("%L", argValue)
                    is List<*> -> {
                        // Handle arrays/lists - join elements
                        val elements = argValue.filterNotNull()
                        if (elements.isNotEmpty()) {
                            val formattedElements = elements.joinToString(", ") { elem ->
                                when (elem) {
                                    is String -> "\"$elem\""
                                    else -> elem.toString()
                                }
                            }
                            builder.addMember(formattedElements)
                        }
                    }
                    // Skip other complex types (like enums, annotations, etc.)
                }
            }
        }

        return builder.build()
    }

    /**
     * Generates Hilt module for the task
     */
    private fun generateHiltModule(
        function: KSFunctionDeclaration,
        taskData: TaskData
    ) {
        val functionNameCapitalized = taskData.functionName.replaceFirstChar { it.uppercase() }
        val moduleName = "${functionNameCapitalized}TaskModule"

        // Generate in function's package + .generated
        val generatedPackageName = "${taskData.packageName}.generated"

        val fileSpec = FileSpec.builder(
            packageName = generatedPackageName,
            fileName = moduleName
        ).apply {
            // Add file-level comment warning not to edit
            addFileComment(
                """
                This file is auto-generated by KSP (Kotlin Symbol Processing).
                DO NOT EDIT THIS FILE MANUALLY - it will be regenerated on each build.

                Generated from: ${taskData.packageName}.${taskData.functionName}
                Task name: ${taskData.taskName}
                """.trimIndent()
            )
            // Add import for the task function
            addImport(taskData.packageName, taskData.functionName)
            addType(buildModuleType(taskData, moduleName))
        }.build()

        // Write to file
        try {
            fileSpec.writeTo(
                codeGenerator = codeGenerator,
                dependencies = Dependencies(false, function.containingFile!!)
            )
            logger.info("Generated Hilt module: $moduleName for function ${taskData.functionName}")
        } catch (e: Exception) {
            logger.error(
                "Failed to generate Hilt module for ${taskData.functionName}: ${e.message}",
                symbol = function
            )
        }
    }

    /**
     * Builds the module TypeSpec
     */
    private fun buildModuleType(taskData: TaskData, moduleName: String): TypeSpec {
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
            .addFunction(buildInitializerFunction(taskData))
            .addFunction(buildInitializerToMapFunction(taskData))
            .build()
    }

    /**
     * Builds the initializer provider function
     */
    private fun buildInitializerFunction(taskData: TaskData): FunSpec {
        val suspendLazyClassName = ClassName("com.github.andock.daemon.utils", "SuspendLazy")
        val suspendLazyMember = MemberName("com.github.andock.daemon.utils", "suspendLazy")

        return FunSpec.builder("initializer")
            .addAnnotation(ClassName("dagger", "Provides"))
            .addAnnotation(ClassName("javax.inject", "Singleton"))
            .addAnnotation(
                AnnotationSpec.builder(ClassName("javax.inject", "Named"))
                    .addMember("%S", taskData.taskName)
                    .build()
            )
            .returns(suspendLazyClassName.parameterizedBy(taskData.returnType))
            .apply {
                // Add transformed parameters
                taskData.parameters.forEach { param ->
                    val paramSpec = if (param.hasAppTask) {
                        // Transform to SuspendLazy<T> with @Named annotation
                        ParameterSpec.builder(
                            param.name,
                            suspendLazyClassName.parameterizedBy(param.type)
                        )
                            .addAnnotation(
                                AnnotationSpec.builder(ClassName("javax.inject", "Named"))
                                    .addMember("%S", param.appTaskName ?: "")
                                    .build()
                            )
                            .build()
                    } else {
                        // Keep original type (with type annotations) and parameter annotations
                        ParameterSpec.builder(param.name, param.type)
                            .apply {
                                param.annotations.forEach { addAnnotation(it) }
                            }
                            .build()
                    }
                    addParameter(paramSpec)
                }
            }
            .addCode(
                buildCodeBlock {
                    add("return %M<%T> {\n", suspendLazyMember, taskData.returnType)
                    indent()
                    add("%N(\n", taskData.functionName)
                    indent()
                    taskData.parameters.forEachIndexed { index, param ->
                        if (param.hasAppTask) {
                            addStatement("%N.getValue()%L", param.name, if (index < taskData.parameters.size - 1) "," else "")
                        } else {
                            addStatement("%N%L", param.name, if (index < taskData.parameters.size - 1) "," else "")
                        }
                    }
                    unindent()
                    add(")\n")
                    unindent()
                    add("}\n")
                }
            )
            .build()
    }

    /**
     * Builds the initializerToMap function
     */
    private fun buildInitializerToMapFunction(taskData: TaskData): FunSpec {
        val suspendLazyClassName = ClassName("com.github.andock.daemon.utils", "SuspendLazy")

        return FunSpec.builder("initializerToMap")
            .addAnnotation(ClassName("dagger", "Provides"))
            .addAnnotation(ClassName("dagger.multibindings", "IntoMap"))
            .addAnnotation(
                AnnotationSpec.builder(ClassName("dagger.multibindings", "StringKey"))
                    .addMember("%S", taskData.taskName)
                    .build()
            )
            .addParameter(
                ParameterSpec.builder(
                    "task",
                    suspendLazyClassName.parameterizedBy(taskData.returnType)
                )
                    .addAnnotation(
                        AnnotationSpec.builder(ClassName("javax.inject", "Named"))
                            .addMember("%S", taskData.taskName)
                            .build()
                    )
                    .build()
            )
            .returns(suspendLazyClassName.parameterizedBy(STAR))
            .addStatement("return task")
            .build()
    }

    /**
     * Data class to hold task information
     */
    private data class TaskData(
        val functionName: String,
        val packageName: String,
        val taskName: String,
        val returnType: TypeName,
        val parameters: List<ParameterData>
    )

    /**
     * Data class to hold parameter information
     */
    private data class ParameterData(
        val name: String,
        val type: TypeName,
        val hasAppTask: Boolean,
        val appTaskName: String?,
        val annotations: List<AnnotationSpec>
    )

    @Suppress("unused")
    @AutoService(SymbolProcessorProvider::class)
    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return AppTaskProcessor(
                codeGenerator = environment.codeGenerator,
                logger = environment.logger
            )
        }
    }
}
