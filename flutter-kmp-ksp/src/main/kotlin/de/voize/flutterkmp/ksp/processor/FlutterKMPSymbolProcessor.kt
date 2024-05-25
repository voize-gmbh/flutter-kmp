package de.voize.flutterkmp.ksp.processor

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

private const val JvmPlatform = "JVM"
private const val NativePlatform = "Native"

class FlutterKMPSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val platforms: List<PlatformInfo>,
    private val options: Map<String, String>,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private var invoked = false

    data class FlutterModule(
        val wrappedClassDeclaration: KSClassDeclaration,
        val moduleName: String,
        val flutterMethods: List<KSFunctionDeclaration>,
        val flutterFlows: List<KSPropertyDeclaration>,
        val flutterStateFlows: List<KSDeclaration>,
        val isInternal: Boolean,
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val platformNames = platforms.map { it.platformName }
        val flutterModuleType =
            resolver.getClassDeclarationByName("$flutterKmpPackageName.annotation.FlutterModule")
                ?.asType(emptyList())
                ?: error("Could not find FlutterModule")

        val functionsByClass =
            resolver.getSymbolsWithAnnotation("$flutterKmpPackageName.annotation.FlutterMethod")
                .map { annotatedNode ->
                    when (annotatedNode) {
                        is KSFunctionDeclaration -> annotatedNode.also {
                            if (annotatedNode.typeParameters.isNotEmpty()) {
                                error("Type parameters are not supported for FlutterMethod")
                            }
                        }

                        else -> throw IllegalArgumentException("FlutterMethod annotation can only be used on function declarations")
                    }
                }.groupBy { annotatedNode ->
                    annotatedNode.parentDeclaration.let {
                        when (it) {
                            is KSClassDeclaration -> it
                            else -> throw IllegalArgumentException("ReactNativeMethods must be declared in a class")
                        }
                    }
                }

        val flowsByClass = resolver.getSymbolsWithAnnotation("$flutterKmpPackageName.annotation.FlutterFlow")
            .map { annotatedNode ->
                when (annotatedNode) {
                    is KSPropertyDeclaration -> annotatedNode.also {
                        if (annotatedNode.typeParameters.isNotEmpty()) {
                            error("Type parameters are not supported for FlutterFlow")
                        }
                    }

                    else -> throw IllegalArgumentException("FlutterFlow annotation can only be used on property declarations")
                }
            }.groupBy { annotatedNode ->
                annotatedNode.parentDeclaration.let {
                    when (it) {
                        is KSClassDeclaration -> it
                        else -> throw IllegalArgumentException("FlutterFlow must be declared in a class")
                    }
                }
            }

        val stateFlowsByClass = resolver.getSymbolsWithAnnotation("$flutterKmpPackageName.annotation.FlutterStateFlow")
            .map { annotatedNode ->
                when (annotatedNode) {
                    is KSPropertyDeclaration -> annotatedNode.also {
                        if (annotatedNode.typeParameters.isNotEmpty()) {
                            error("Type parameters are not supported for FlutterStateFlow")
                        }
                    }

                    is KSFunctionDeclaration -> annotatedNode.also {
                        if (annotatedNode.typeParameters.isNotEmpty()) {
                            error("Type parameters are not supported for FlutterStateFlow")
                        }
                    }

                    else -> throw IllegalArgumentException("FlutterStateFlow annotation can only be used on property or method declarations")
                }
            }.groupBy { annotatedNode ->
                annotatedNode.parentDeclaration.let {
                    when (it) {
                        is KSClassDeclaration -> it
                        else -> throw IllegalArgumentException("FlutterStateFlow must be declared in a class")
                    }
                }
            }

        val flutterModules =
            resolver.getSymbolsWithAnnotation("$flutterKmpPackageName.annotation.FlutterModule")
                .map { annotatedNode ->
                    when (annotatedNode) {
                        is KSClassDeclaration -> annotatedNode.also {
                            if (annotatedNode.typeParameters.isNotEmpty()) {
                                error("Type parameters are not supported for FlutterModule")
                            }
                        }
                        else -> throw IllegalArgumentException("FlutterModule annotation can only be used on class declarations")
                    }
                }.map { wrappedClassDeclaration ->
                    val flutterModuleAnnotationArguments =
                        wrappedClassDeclaration.annotations.single { it.annotationType.resolve() == flutterModuleType }.arguments
                    val moduleName = flutterModuleAnnotationArguments.single {
                        it.name?.asString() == "name"
                    }.value as String

                    val flutterMethods = functionsByClass[wrappedClassDeclaration].orEmpty()
                    val flutterFlows = flowsByClass[wrappedClassDeclaration].orEmpty()
                    val flutterStateFlows = stateFlowsByClass[wrappedClassDeclaration].orEmpty()
                    val isInternal = false // wrappedClassDeclaration.modifiers.contains(Modifier.INTERNAL)

                    FlutterModule(
                        wrappedClassDeclaration = wrappedClassDeclaration,
                        moduleName = moduleName,
                        flutterMethods = flutterMethods,
                        flutterFlows = flutterFlows,
                        flutterStateFlows = flutterStateFlows,
                        isInternal = isInternal,
                    )
                }.toList()

        flutterModules.forEach { flutterModule ->
            val wrappedClassDeclaration = flutterModule.wrappedClassDeclaration
            val wrappedClassType = wrappedClassDeclaration.asType(emptyList()).toTypeName()

            val packageName = wrappedClassDeclaration.packageName.asString()
            val wrappedClassName = wrappedClassDeclaration.simpleName.asString()

            val primaryConstructorParameters =
                wrappedClassDeclaration.primaryConstructor?.parameters
                    ?: emptyList()
            val constructorInvocationArguments =
                primaryConstructorParameters.map { constructorParameter ->
                    CodeBlock.of("%N", constructorParameter.name?.asString())
                }.joinToCode()
            val constructorInvocation =
                CodeBlock.of("%T(%L)", wrappedClassType, constructorInvocationArguments)

            val constructorParameters = primaryConstructorParameters.map { it.toParameterSpec() }

            if (JvmPlatform in platformNames && NativePlatform !in platformNames) {
                createAndroidModule(flutterModule, constructorParameters, constructorInvocation)
            }
        }

        if (!invoked && JvmPlatform in platformNames && NativePlatform in platformNames) {
            DartGenerator(codeGenerator).generate(flutterModules)
        }

        invoked = true
        return emptyList()
    }

    private fun String.androidModuleClassName() = this + "Android"

    private fun createAndroidModule(
        flutterModule: FlutterModule,
        constructorParameters: List<ParameterSpec>,
        constructorInvocation: CodeBlock,
    ) {
        val packageName = flutterModule.wrappedClassDeclaration.packageName.asString()
        val wrappedClassName = flutterModule.wrappedClassDeclaration.simpleName.asString()
        val className = wrappedClassName.androidModuleClassName()
        val wrappedModuleVarName = "wrappedModule"
        val methodChannelVarName = "methodChannel"
        val eventChannelsVarName = "eventChannels"
        val binaryMessengerConstructorArgName = "binaryMessenger"

        val classSpec = TypeSpec.classBuilder(className).apply {
            if (flutterModule.isInternal) {
                addModifiers(KModifier.INTERNAL)
            }
            primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(
                        binaryMessengerConstructorArgName,
                        BinaryMessenger,
                    )
                    .addParameters(constructorParameters)
                    .build()
            )
            addSuperinterface(MethodCallHandler)
            addProperty(
                PropertySpec.builder(
                    wrappedModuleVarName,
                    ClassName(packageName, wrappedClassName)
                ).addModifiers(KModifier.PRIVATE).initializer(constructorInvocation).build()
            )
            addProperty(
                PropertySpec.builder(
                    methodChannelVarName,
                    MessageChannel,
                ).addModifiers(KModifier.PRIVATE).initializer(
                    CodeBlock.of(
                        "%T(%N, %S)",
                        MessageChannel,
                        binaryMessengerConstructorArgName,
                        flutterModule.moduleName
                    )
                ).build()
            )
            addProperty(
                PropertySpec.builder(
                    eventChannelsVarName,
                    List::class.asClassName().parameterizedBy(EventChannel),
                ).addModifiers(KModifier.PRIVATE).initializer(
                    CodeBlock.of(
                        "listOf(%L)",
                        flutterModule.flutterFlows.map { flow ->
                            CodeBlock.of(
                                "%T(%N, %S).also { it.setStreamHandler(%N.%M.%M()) }",
                                EventChannel,
                                binaryMessengerConstructorArgName,
                                "${flutterModule.moduleName}_${flow.simpleName.asString()}",
                                wrappedModuleVarName,
                                MemberName(packageName, flow.simpleName.asString()),
                                toEventStreamHandler,
                            )
                        }.joinToCode()
                    )
                ).build()
            )
            addInitializerBlock(
                CodeBlock.builder().apply {
                    addStatement("%N.setMethodCallHandler(this)", methodChannelVarName)
                }.build()
            )
            addFunction(
                FunSpec.builder("onMethodCall")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("call", MethodCall)
                    .addParameter("result", MethodChannelResult)
                    .addCode(
                        CodeBlock.builder().apply {
                            beginControlFlow("when (call.method)")
                            flutterModule.flutterMethods.forEach { method ->
                                addMethodCodeBlock(method, wrappedModuleVarName)
                            }
                            flutterModule.flutterStateFlows.forEach { stateFlow ->
                                addStateFlowCodeBlock(stateFlow, wrappedModuleVarName)
                            }
                            addStatement("else -> result.notImplemented()")
                            endControlFlow()
                        }.build()
                    )
                    .build()
            )
        }.build()

        val fileSpec = FileSpec.builder(packageName, className)
            .addFileComment("Generated by flutter-kmp. Do not modify.")
            .addType(classSpec)
            .build()

        fileSpec.writeTo(codeGenerator, false)
    }


    /**
     * ```
     * "myFlow" -> {
     *      val arguments = call.arguments as List<*>
     *      val previous = arguments[0] as Int
     *
     *      CoroutineScope(Dispatchers.Default).launch {
     *          val next = wrappedModule.counter.first {
     *              it != previous
     *          }
     *          result.success(next)
     *      }
     * }
     * ```
     */
    private fun CodeBlock.Builder.addStateFlowCodeBlock(
        stateFlow: KSDeclaration,
        wrappedModuleVarName: String,
    ) {
        val flowTypeArgument = stateFlow.getStateFlowDeclarationFlowTypeArgument()
        val parameters = when (stateFlow) {
            is KSPropertyDeclaration -> emptyList()
            is KSFunctionDeclaration -> stateFlow.parameters
            else -> error("only property and function declaration allowed for @FlutterStateFlow")
        }

        beginControlFlow("%S ->", stateFlow.simpleName.asString())

        addStatement("val arguments = call.arguments as List<*>")

        if (
            flowTypeArgument.declaration.requiresSerialization() ||
            listOf(
                "kotlinx.datetime.Instant",
                "kotlinx.datetime.LocalDateTime",
                "kotlinx.datetime.LocalDate",
                "kotlinx.datetime.LocalTime",
                "kotlin.time.Duration",
            ).contains(flowTypeArgument.declaration.qualifiedName?.asString())
        ) {
            addStatement("val previous = arguments[0] as String?")
        } else {
            addStatement(
                "val previous = arguments[0] as %T",
                flowTypeArgument.makeNullable().toTypeName(),
            )
        }

        parameters.forEachIndexed { index, parameter ->
            getKotlinDeserialization(
                parameter.type.resolve(),
                "arguments[${index + 1}]",
                "param$index",
            )
        }

        beginControlFlow(
            "%T(%T.Default).%M",
            CoroutineScope,
            Dispatchers,
            launch,
        )

        if (flowTypeArgument.declaration.requiresSerialization()) {
            addStatement("val json = %T { encodeDefaults = true }", JsonClassName)
        }

        when (stateFlow) {
            is KSPropertyDeclaration -> beginControlFlow(
                "val next = %N.%L.%M",
                wrappedModuleVarName,
                stateFlow.simpleName.asString(),
                first,
            )
            is KSFunctionDeclaration -> beginControlFlow(
                "val next = %N.%L(%L).%M",
                wrappedModuleVarName,
                stateFlow.simpleName.asString(),
                List(parameters.size) { index -> "param$index" }.joinToString(", "),
                first,
            )
            else -> error("only property and function declaration allowed for @FlutterStateFlow")
        }

        if (flowTypeArgument.declaration.requiresSerialization()) {
            addStatement("%L.encodeToString(it) != previous", "json")
        }else if (
            listOf(
                "kotlinx.datetime.Instant",
                "kotlinx.datetime.LocalDateTime",
                "kotlinx.datetime.LocalDate",
                "kotlinx.datetime.LocalTime",
                "kotlin.time.Duration",
            ).contains(flowTypeArgument.declaration.qualifiedName?.asString())
        ) {
            addStatement("%T.encodeToString(it) != previous", JsonClassName)
        } else {
            addStatement("it != previous")
        }
        endControlFlow()

        getKotlinSerialization(flowTypeArgument.declaration, "next", "json")

        endControlFlow()
        endControlFlow()
    }

    private fun CodeBlock.Builder.addMethodCodeBlock(
        method: KSFunctionDeclaration,
        wrappedModuleVarName: String,
    ) {
        beginControlFlow("%S ->", method.simpleName.asString())

        if (method.parameters.isNotEmpty()) {
            //
            // val arguments = call.arguments as List<*>
            // val arg0 = arguments[0] as Arg0Type
            // ...
            //
            addStatement("val arguments = call.arguments as List<*>")
            method.parameters.forEachIndexed { index, parameter ->
                getKotlinDeserialization(
                    parameter.type.resolve(),
                    "arguments[$index]",
                    "arg$index",
                )
            }
        }

        fun withCoroutineScopeControlFlow(block: CodeBlock.Builder.() -> Unit) {
            beginControlFlow(
                "%T(%T.Default).%M",
                CoroutineScope,
                Dispatchers,
                launch,
            )
            block()
            endControlFlow()
        }

        fun invokeUnitMethodStatement() {
            addStatement(
                "%N.%L(%L)",
                wrappedModuleVarName,
                method.simpleName.asString(),
                method.parameters.indices.joinToString(", ") { "arg$it" }
            )
        }

        fun invokeMethodStatement() {
            addStatement(
                "val resultData = %N.%L(%L)",
                wrappedModuleVarName,
                method.simpleName.asString(),
                method.parameters.indices.joinToString(", ") { "arg$it" }
            )

            val returnTypeDeclaration = (method.returnType ?: error("return type is null")).resolve().declaration

            addStatement("val json = %T { encodeDefaults = true }", JsonClassName)
            getKotlinSerialization(returnTypeDeclaration, "resultData", "json")
        }

        fun unitResultSuccessStatement() {
            addStatement("result.success(null)")
        }

        if (method.returnsUnit()) {
            if (method.modifiers.contains(Modifier.SUSPEND)) {
                //
                // CoroutineScope(Dispatchers.Default).launch {
                //      wrappedModule.method(arg0, arg1, ...)
                //      result.success(null)
                // }
                //
                withCoroutineScopeControlFlow {
                    invokeUnitMethodStatement()
                    unitResultSuccessStatement()
                }

            } else {
                //
                // wrappedModule.method(arg0, arg1, ...)
                // result.success(null)
                //
                invokeUnitMethodStatement()
                unitResultSuccessStatement()
            }
        } else {
            if (method.modifiers.contains(Modifier.SUSPEND)) {
                //
                // CoroutineScope(Dispatchers.Default).launch {
                //      result.success(wrappedModule.method())
                // }
                //
                withCoroutineScopeControlFlow {
                    invokeMethodStatement()
                }
            } else {
                //
                // result.success(wrappedModule.method())
                //
                invokeMethodStatement()
            }
        }
        endControlFlow()
    }
}

@AutoService(SymbolProcessorProvider::class)
class ToolkitSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return FlutterKMPSymbolProcessor(
            environment.codeGenerator,
            environment.platforms,
            environment.options,
            environment.logger
        )
    }
}

fun KSValueParameter.toParameterSpec(): ParameterSpec {
    return ParameterSpec.builder(
        this.name?.asString() ?: error("Parameter must have a name"),
        this.type.toTypeName()
    )
        .build()
}

fun CodeBlock.Builder.getKotlinSerialization(
    declaration: KSDeclaration,
    varName: String,
    jsonInstanceVarName: String? = null,
) {
    if (declaration.requiresSerialization()) {
        require(jsonInstanceVarName != null)
        addStatement(
            "result.success(%L.%M(%L))",
            jsonInstanceVarName,
            encodeToString,
            varName,
        )
    } else when (declaration.qualifiedName?.asString()) {
        "kotlinx.datetime.Instant",
        "kotlinx.datetime.LocalDateTime",
        "kotlinx.datetime.LocalTime",
        "kotlinx.datetime.LocalDate" -> addStatement("result.success(%L.toString())", varName)
        "kotlin.time.Duration" -> addStatement("result.success(%L.toIsoString())", varName)
        else -> addStatement("result.success(%L)", varName)
    }
}

fun CodeBlock.Builder.getKotlinDeserialization(type: KSType, varName: String, assignTo: String) {
    if (type.declaration.requiresSerialization()) {
        addStatement(
            "val %L = %T.decodeFromString<%T>(%L as String)",
            assignTo,
            JsonClassName,
            type.toTypeName(),
            varName,
        )
    } else when (type.declaration.qualifiedName?.asString()) {
        "kotlinx.datetime.Instant" -> {
            addStatement(
                "val %L = %T.parse(%L as String)",
                assignTo,
                Instant,
                varName,
            )
        }
        "kotlinx.datetime.LocalDateTime" -> {
            addStatement(
                "val %L = %T.parse(%L as String)",
                assignTo,
                LocalDateTime,
                varName,
            )
        }
        "kotlinx.datetime.LocalDate" -> {
            addStatement(
                "val %L = %T.parse(%L as String)",
                assignTo,
                LocalDate,
                varName,
            )
        }
        "kotlinx.datetime.LocalTime" -> {
            addStatement(
                "val %L = %T.parse(%L as String)",
                assignTo,
                LocalTime,
                varName,
            )
        }
        "kotlin.time.Duration" -> {
            addStatement(
                "val %L = %T.parse(%L as String)",
                assignTo,
                Duration,
                varName,
            )
        }
        else -> addStatement(
            "val %L = %L as %T",
            assignTo,
            varName,
            type.toTypeName(),
        )
    }
}

private const val flutterKmpPackageName = "de.voize.flutterkmp"

private val MethodCallHandler = ClassName("io.flutter.plugin.common.MethodChannel", "MethodCallHandler")
private val BinaryMessenger = ClassName("io.flutter.plugin.common", "BinaryMessenger")
private val MessageChannel = ClassName("io.flutter.plugin.common", "MethodChannel")
private val EventChannel = ClassName("io.flutter.plugin.common", "EventChannel")
private val MethodCall = ClassName("io.flutter.plugin.common", "MethodCall")
private val MethodChannelResult = ClassName("io.flutter.plugin.common.MethodChannel", "Result")
private val toEventStreamHandler = MemberName(flutterKmpPackageName, "toEventStreamHandler")

private val Duration = ClassName("kotlin.time", "Duration")
private val Instant = ClassName("kotlinx.datetime", "Instant")
private val LocalDate = ClassName("kotlinx.datetime", "LocalDate")
private val LocalTime = ClassName("kotlinx.datetime", "LocalTime")
private val LocalDateTime = ClassName("kotlinx.datetime", "LocalDateTime")
private val Dispatchers = ClassName("kotlinx.coroutines", "Dispatchers")
private val launch = MemberName("kotlinx.coroutines", "launch")
private val CoroutineScope = ClassName("kotlinx.coroutines", "CoroutineScope")
private val ListOfMember = MemberName("kotlin.collections", "listOf")
private val JsonClassName = ClassName("kotlinx.serialization.json", "Json")
private val encodeToString = MemberName("kotlinx.serialization", "encodeToString")
private val first = MemberName("kotlinx.coroutines.flow", "first")
