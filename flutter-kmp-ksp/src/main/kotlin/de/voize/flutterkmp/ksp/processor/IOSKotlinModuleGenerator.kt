package de.voize.flutterkmp.ksp.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
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
        val registrarConstructorArgName = "registrar"
        val pluginInstanceConstructorArgName = "pluginInstance"
        val createMethodChannelArgName = "createMethodChannel"
        val createEventChannelArgName = "createEventChannel"
        val createFlutterErrorArgName = "createFlutterError"
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
                    .addParameter(
                        createMethodChannelArgName,
                        LambdaTypeName.get(
                            parameters = listOf(
                                ParameterSpec("name", STRING),
                                ParameterSpec("binaryMessenger", NSObject),
                            ),
                            returnType = FlutterMethodChannel,
                        )
                    )
                    .addParameter(
                        createEventChannelArgName,
                        LambdaTypeName.get(
                            parameters = listOf(
                                ParameterSpec("name", STRING),
                                ParameterSpec("binaryMessenger", NSObject),
                            ),
                            returnType = FlutterEventChannel,
                        )
                    )
                    .addParameter(
                        createFlutterErrorArgName,
                        LambdaTypeName.get(
                            parameters = listOf(
                                ParameterSpec("code", STRING),
                                ParameterSpec("message", STRING.copy(nullable = true)),
                                ParameterSpec("details", ANY.copy(nullable = true)),
                            ),
                            returnType = FlutterError,
                        )
                    )
                    .addCode(
                        CodeBlock.builder().apply {
                            addStatement(
                                "val $methodChannelVarName = %L(%S, %L ?: error(%S))",
                                createMethodChannelArgName,
                                flutterModule.moduleName,
                                "$registrarConstructorArgName.messenger()",
                                "$registrarConstructorArgName.messenger() is null"
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
                                    "%L(%S, %L ?: error(%S)).setStreamHandler(%N.%M.%M(%L))",
                                    createEventChannelArgName,
                                    "${flutterModule.moduleName}_${it.simpleName.asString()}",
                                    "$registrarConstructorArgName.messenger()",
                                    "$registrarConstructorArgName.messenger() is null",
                                    wrappedModuleVarName,
                                    MemberName(packageName, it.simpleName.asString()),
                                    toEventStreamHandler,
                                    createFlutterErrorArgName,
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
                                    method = method,
                                    moduleName = flutterModule.moduleName,
                                    wrappedModuleVarName = wrappedModuleVarName,
                                    resultStatement = { resultParameter ->
                                        "result!!($resultParameter)"
                                    },
                                    append = { addStatement("true") }
                                )
                            }
                            flutterModule.flutterStateFlows.forEach { stateFlow ->
                                addStateFlowCodeBlock(
                                    stateFlow = stateFlow,
                                    wrappedModuleVarName = wrappedModuleVarName,
                                    moduleName = flutterModule.moduleName,
                                    resultStatement = { resultParameter ->
                                        "result!!($resultParameter)"
                                    },
                                    append = { addStatement("true") }
                                )
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
}

private val ExperimentalForeignApi = ClassName("kotlinx.cinterop", "ExperimentalForeignApi")
private val NSObject = ClassName("platform.darwin", "NSObject")

private val FlutterResult = ClassName("flutter", "FlutterResult")
private val FlutterMethodCall = ClassName("flutter", "FlutterMethodCall")
private val FlutterMethodChannel = ClassName("flutter", "FlutterMethodChannel")
private val FlutterEventChannel = ClassName("flutter", "FlutterEventChannel")
private val FlutterError = ClassName("flutter", "FlutterError")
private val FlutterPluginRegistrar = ClassName("flutter", "FlutterPluginRegistrarProtocol")
private val FlutterPlugin = ClassName("flutter", "FlutterPluginProtocol")
private val toEventStreamHandler = MemberName(flutterKmpPackageName, "toEventStreamHandler")