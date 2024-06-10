package de.voize.flutterkmp.ksp.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class AndroidModuleGenerator {
    private fun String.androidModuleClassName() = this + "Android"

    fun generateModule(
        flutterModule: FlutterKMPSymbolProcessor.FlutterModule,
        constructorParameters: List<KSValueParameter>,
        constructorInvocation: CodeBlock,
        codeGenerator: CodeGenerator,
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
                    .addParameters(constructorParameters.map { it.toParameterSpec() })
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

    fun KSValueParameter.toParameterSpec(): ParameterSpec {
        return ParameterSpec.builder(
            this.name?.asString() ?: error("Parameter must have a name"),
            this.type.toTypeName()
        )
            .build()
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
}

private fun CodeBlock.Builder.getKotlinSerialization(
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

private fun CodeBlock.Builder.getKotlinDeserialization(type: KSType, varName: String, assignTo: String) {
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
