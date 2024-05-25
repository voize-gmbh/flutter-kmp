package de.voize.flutterkmp.ksp.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
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
    )  {
        val packageName = flutterModule.wrappedClassDeclaration.packageName.asString()
        val wrappedClassName = flutterModule.wrappedClassDeclaration.simpleName.asString()
        val className = wrappedClassName.androidModuleClassName()
        val wrappedModuleVarName = "wrappedModule"
        val eventChannelsVarName = "eventChannels"
        val binaryMessengerConstructorArgName = "binaryMessenger"

        val classSpec = TypeSpec.classBuilder(className).apply {
            if (flutterModule.isInternal) {
                addModifiers(KModifier.INTERNAL)
            }
            superclass(MethodChannel)
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
            addSuperclassConstructorParameter(
                CodeBlock.of("%N, %S", binaryMessengerConstructorArgName, flutterModule.moduleName)
            )
            addProperty(
                PropertySpec.builder(
                    wrappedModuleVarName,
                    ClassName(packageName, wrappedClassName)
                ).addModifiers(KModifier.PRIVATE).initializer(constructorInvocation).build()
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
                    addStatement("setMethodCallHandler(this)")
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
        resultStatement: (resultParameter: String) -> String = { "result.success($it)" },
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

        getKotlinSerialization(
            flowTypeArgument.declaration,
            "next",
            "serializedNext",
            "json"
        )
        addStatement(resultStatement("serializedNext"))

        endControlFlow()
        endControlFlow()
    }
}

private val MethodCallHandler = ClassName("io.flutter.plugin.common.MethodChannel", "MethodCallHandler")
private val BinaryMessenger = ClassName("io.flutter.plugin.common", "BinaryMessenger")
private val MethodChannel = ClassName("io.flutter.plugin.common", "MethodChannel")
private val EventChannel = ClassName("io.flutter.plugin.common", "EventChannel")
private val MethodCall = ClassName("io.flutter.plugin.common", "MethodCall")
private val MethodChannelResult = ClassName("io.flutter.plugin.common.MethodChannel", "Result")
private val toEventStreamHandler = MemberName(flutterKmpPackageName, "toEventStreamHandler")
