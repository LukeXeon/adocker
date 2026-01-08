package com.github.andock.startup.ksp

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
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class TaskProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    companion object {
        private const val TASK_ANNOTATION = "com.github.andock.startup.Task"
        private const val TRIGGER_ANNOTATION = "com.github.andock.startup.Trigger"
    }

    /**
     * Helper function to check if annotation matches the expected qualified name
     */
    private fun KSAnnotation.isAnnotation(qualifiedName: String): Boolean {
        val annotationType = this.annotationType.resolve()
        val declaration = annotationType.declaration
        val actualQualifiedName = declaration.qualifiedName?.asString()
        return actualQualifiedName == qualifiedName
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Find all symbols annotated with @Task
        val taskAnnotatedSymbols = resolver
            .getSymbolsWithAnnotation(TASK_ANNOTATION)
            .filterIsInstance<KSFunctionDeclaration>()

        taskAnnotatedSymbols.forEach { function ->
            // Validate function meets requirements for @Task annotation
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
     * Validates that the function meets requirements for @Task annotation
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
                "@Task annotation can only be used on top-level functions or functions inside an object. " +
                        "Function ${function.simpleName.asString()} is inside ${parent.simpleName.asString()} which is not an object.",
                symbol = function
            )
            isValid = false
        }

        return isValid
    }

    /**
     * Extracts task data from @Task annotation
     */
    private fun extractTaskData(function: KSFunctionDeclaration): TaskData? {
        val taskAnnotation = function.annotations.firstOrNull {
            it.isAnnotation(TASK_ANNOTATION)
        } ?: return null

        // Extract task name
        val taskNameArg = taskAnnotation.arguments
            .firstOrNull { it.name?.asString() == "name" }

        if (taskNameArg == null) {
            logger.error(
                "@Task annotation must specify a 'name' parameter",
                symbol = function
            )
            return null
        }

        val taskName = taskNameArg.value as? String
        if (taskName == null) {
            logger.error(
                "@Task annotation 'name' parameter must be a valid string",
                symbol = function
            )
            return null
        }

        val triggerKey = function.annotations.firstOrNull {
            it.isAnnotation(TRIGGER_ANNOTATION)
        }?.arguments?.firstOrNull {
            it.name?.asString() == "value"
        }?.value as? String ?: ""

        // Extract return type
        val returnType = function.returnType?.resolve()?.toTypeName() ?: run {
            logger.error(
                "@Task function ${function.simpleName.asString()} must have a return type",
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
            triggerKey = triggerKey,
            returnType = returnType,
            parameters = parameters
        )
    }

    /**
     * Extracts parameter data including @Task annotation if present
     */
    private fun extractParameterData(parameter: KSValueParameter): ParameterData {
        // Check if parameter has @Task annotation
        val taskAnnotation = parameter.annotations.firstOrNull {
            it.isAnnotation(TASK_ANNOTATION)
        }

        val hasTask = taskAnnotation != null
        val taskName = if (hasTask) {
            taskAnnotation.arguments
                .firstOrNull { it.name?.asString() == "name" }
                ?.value as? String
        } else null

        // Collect other annotations (excluding @Task)
        val otherAnnotations = parameter.annotations
            .filter { !it.isAnnotation(TASK_ANNOTATION) }
            .map { convertToAnnotationSpec(it) }
            .toList()

        // Get parameter type preserving all annotations including type argument annotations
        val resolvedType = parameter.type.resolve()
        val baseType = resolvedType.toTypeName()

        // Try to preserve type argument annotations for parameterized types
        val paramType =
            if (baseType is ParameterizedTypeName && resolvedType.arguments.isNotEmpty()) {
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
            hasTask = hasTask,
            taskName = taskName,
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
        val suspendLazyClassName = ClassName("com.github.andock.startup", "SuspendLazy")
        val suspendLazyMember = MemberName("com.github.andock.startup", "suspendLazy")
        val measureTimeMillisWithResultMember =
            MemberName("com.github.andock.startup", "measureTimeMillisWithResult")
        val pairClassName = ClassName("kotlin", "Pair")

        return FunSpec.builder("initializer")
            .addAnnotation(ClassName("dagger", "Provides"))
            .addAnnotation(ClassName("javax.inject", "Singleton"))
            .addAnnotation(
                AnnotationSpec.builder(ClassName("javax.inject", "Named"))
                    .addMember("%S", taskData.taskName)
                    .build()
            )
            .returns(
                suspendLazyClassName.parameterizedBy(
                    pairClassName.parameterizedBy(
                        taskData.returnType,
                        ClassName("kotlin", "Long")
                    )
                )
            )
            .apply {
                // Add transformed parameters
                taskData.parameters.forEach { param ->
                    val paramSpec = if (param.hasTask) {
                        // Transform to SuspendLazy<Pair<T, Long>> with @Named annotation
                        val typeWithAnnotation = suspendLazyClassName.parameterizedBy(
                            pairClassName.parameterizedBy(
                                param.type,
                                ClassName("kotlin", "Long")
                            )
                        ).copy(
                            annotations = listOf(
                                AnnotationSpec.builder(
                                    ClassName(
                                        "kotlin.jvm",
                                        "JvmSuppressWildcards"
                                    )
                                ).build()
                            )
                        )

                        ParameterSpec.builder(param.name, typeWithAnnotation)
                            .addAnnotation(
                                AnnotationSpec.builder(ClassName("javax.inject", "Named"))
                                    .addMember("%S", param.taskName ?: "")
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
                    add(
                        "return %M<%T> {\n",
                        suspendLazyMember,
                        pairClassName.parameterizedBy(
                            taskData.returnType,
                            ClassName("kotlin", "Long")
                        )
                    )
                    indent()

                    // Generate local variables for @Task parameters before measureTimeMillisWithResult
                    taskData.parameters.forEach { param ->
                        if (param.hasTask) {
                            addStatement("val %N = %N.getValue().first", param.name, param.name)
                        }
                    }

                    add("%M {\n", measureTimeMillisWithResultMember)
                    indent()
                    add("%N(\n", taskData.functionName)
                    indent()
                    taskData.parameters.forEachIndexed { index, param ->
                        addStatement(
                            "%N%L",
                            param.name,
                            if (index < taskData.parameters.size - 1) "," else ""
                        )
                    }
                    unindent()
                    add(")\n")
                    unindent()
                    add("}\n")
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
        val suspendLazyClassName = ClassName("com.github.andock.startup", "SuspendLazy")
        val pairClassName = ClassName("kotlin", "Pair")

        return FunSpec.builder("initializerToMap")
            .addAnnotation(ClassName("dagger", "Provides"))
            .addAnnotation(ClassName("dagger.multibindings", "IntoMap"))
            .addAnnotation(
                AnnotationSpec.builder(ClassName("com.github.andock.startup", "TaskInfo"))
                    .addMember("%S", taskData.taskName)
                    .addMember("%S", taskData.triggerKey)
                    .build()
            )
            .addParameter(
                ParameterSpec.builder(
                    "task",
                    ClassName("dagger", "Lazy").parameterizedBy(
                        suspendLazyClassName.parameterizedBy(
                            pairClassName.parameterizedBy(
                                taskData.returnType,
                                ClassName("kotlin", "Long")
                            )
                        )
                    ).copy(
                        annotations = listOf(
                            AnnotationSpec.builder(
                                ClassName(
                                    "kotlin.jvm",
                                    "JvmSuppressWildcards"
                                )
                            ).build()
                        )
                    )
                )
                    .addAnnotation(
                        AnnotationSpec.builder(ClassName("javax.inject", "Named"))
                            .addMember("%S", taskData.taskName)
                            .build()
                    )
                    .build()
            )
            .returns(
                suspendLazyClassName.parameterizedBy(
                    ClassName("kotlin", "Long")
                )
            )
            .addStatement(
                "return %M<%T>(%T.PUBLICATION) { task.get().getValue().second }",
                MemberName("com.github.andock.startup", "suspendLazy"),
                ClassName("kotlin", "Long"),
                ClassName("kotlin", "LazyThreadSafetyMode")
            )
            .build()
    }

    /**
     * Data class to hold task information
     */
    private data class TaskData(
        val functionName: String,
        val packageName: String,
        val taskName: String,
        val triggerKey: String,
        val returnType: TypeName,
        val parameters: List<ParameterData>
    )

    /**
     * Data class to hold parameter information
     */
    private data class ParameterData(
        val name: String,
        val type: TypeName,
        val hasTask: Boolean,
        val taskName: String?,
        val annotations: List<AnnotationSpec>
    )

    @Suppress("unused")
    @AutoService(SymbolProcessorProvider::class)
    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return TaskProcessor(
                codeGenerator = environment.codeGenerator,
                logger = environment.logger
            )
        }
    }
}
