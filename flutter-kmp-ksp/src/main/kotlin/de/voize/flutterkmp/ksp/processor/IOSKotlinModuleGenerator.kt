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
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class IOSKotlinModuleGenerator {
    private fun String.iosModuleClassName() = this + "IOS"

    fun generateModule(
        flutterModule: FlutterKMPSymbolProcessor.FlutterModule,
        constructorParameters: List<KSValueParameter>,
        constructorInvocation: CodeBlock,
        codeGenerator: CodeGenerator,
    ) {
        val packageName = flutterModule.wrappedClassDeclaration.packageName.asString()
        val wrappedClassName = flutterModule.wrappedClassDeclaration.simpleName.asString()
        val className = wrappedClassName.iosModuleClassName()
        val wrappedModuleVarName = "wrappedModule"
        val pluginInstanceConstructorArgName = "pluginInstance"
        val registrarConstructorArgName = "registrar"
        val methodChannelVarName = "methodChannel"

        val classSpec = TypeSpec.classBuilder(className).apply {
            addAnnotation(ExperimentalForeignApi)
            if (flutterModule.isInternal) {
                addModifiers(KModifier.INTERNAL)
            }
            primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameters(constructorParameters.map { it.toParameterSpec() })
                    .build()
            )
            addProperty(
                PropertySpec.builder(
                    wrappedModuleVarName,
                    ClassName(packageName, wrappedClassName)
                ).addModifiers(KModifier.PRIVATE).initializer(constructorInvocation).build()
            )
            .addFunction(
                FunSpec.builder("register")
                    .addParameter(
                        registrarConstructorArgName,
                        FlutterPluginRegistrar,
                    )
                    .addParameter(pluginInstanceConstructorArgName, FlutterPlugin)
                    .addCode(
                        CodeBlock.builder().apply {
                            addStatement(
                                "val $methodChannelVarName = %T(%S, %L, %T.sharedInstance())",
                                FlutterMethodChannel,
                                flutterModule.moduleName,
                                "$registrarConstructorArgName.messenger()",
                                FlutterStandardMethodCodec
                            )
                            addStatement(
                                "%L.addMethodCallDelegate(%L as %T, %L)",
                                registrarConstructorArgName,
                                pluginInstanceConstructorArgName,
                                NSObject,
                                methodChannelVarName,
                            )
                            flutterModule.flutterFlows.forEach {
                                addStatement(
                                    "%T(%S, %L, %T.sharedInstance()).setStreamHandler(%N.%M.%M())",
                                    FlutterEventChannel,
                                    "${flutterModule.moduleName}_${it.simpleName.asString()}",
                                    "$registrarConstructorArgName.messenger()",
                                    FlutterStandardMethodCodec,
                                    wrappedModuleVarName,
                                    MemberName(packageName, it.simpleName.asString()),
                                    toEventStreamHandler,
                                )
                            }
                        }.build()
                    )
                    .build()
            )
            addFunction(
                FunSpec.builder("handleMethodCall")
                    .addParameter("call", FlutterMethodCall)
                    .addParameter("result", FlutterResult)
                    .returns(Boolean::class)
                    .addCode(
                        CodeBlock.builder().apply {
                            beginControlFlow("return when (call.method)")
                            flutterModule.flutterMethods.forEach { method ->
                                addMethodCodeBlock(
                                    method,
                                    wrappedModuleVarName,
                                    resultStatement = { resultParameter ->
                                        "result!!($resultParameter)"
                                    },
                                    append = { addStatement("true") }
                                )
                            }
                            flutterModule.flutterStateFlows.forEach { stateFlow ->
                                addStateFlowCodeBlock(stateFlow, wrappedModuleVarName)
                            }
                            addStatement("else -> false")
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
     *          result!!(next)
     *      }
     *
     *      true
     * }
     * ```
     */
    private fun CodeBlock.Builder.addStateFlowCodeBlock(
        stateFlow: KSDeclaration,
        wrappedModuleVarName: String,
        resultStatement: (resultParameter: String) -> String = { "result!!($it)" },
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

        addStatement("true")

        endControlFlow()
    }
}

private val ExperimentalForeignApi = ClassName("kotlinx.cinterop", "ExperimentalForeignApi")
private val NSObject = ClassName("platform.darwin", "NSObject")

private val FlutterResult = ClassName("cocoapods.Flutter", "FlutterResult")
private val FlutterMethodCall = ClassName("cocoapods.Flutter", "FlutterMethodCall")
private val FlutterMethodChannel = ClassName("cocoapods.Flutter", "FlutterMethodChannel")
private val FlutterEventChannel = ClassName("cocoapods.Flutter", "FlutterEventChannel")
private val FlutterPluginRegistrar = ClassName("cocoapods.Flutter", "FlutterPluginRegistrarProtocol")
private val FlutterPlugin = ClassName("cocoapods.Flutter", "FlutterPluginProtocol")
private val FlutterStandardMethodCodec = ClassName("cocoapods.Flutter", "FlutterStandardMethodCodec")
private val toEventStreamHandler = MemberName(flutterKmpPackageName, "toEventStreamHandler")