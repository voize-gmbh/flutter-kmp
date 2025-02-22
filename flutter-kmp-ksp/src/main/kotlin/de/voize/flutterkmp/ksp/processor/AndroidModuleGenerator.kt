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
                                addMethodCodeBlock(
                                    method = method,
                                    moduleName = flutterModule.moduleName,
                                    wrappedModuleVarName = wrappedModuleVarName,
                                )
                            }
                            flutterModule.flutterStateFlows.forEach { stateFlow ->
                                addStateFlowCodeBlock(
                                    stateFlow = stateFlow,
                                    moduleName = flutterModule.moduleName,
                                    wrappedModuleVarName = wrappedModuleVarName,
                                )
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

}

private val MethodCallHandler = ClassName("io.flutter.plugin.common.MethodChannel", "MethodCallHandler")
private val BinaryMessenger = ClassName("io.flutter.plugin.common", "BinaryMessenger")
private val MethodChannel = ClassName("io.flutter.plugin.common", "MethodChannel")
private val EventChannel = ClassName("io.flutter.plugin.common", "EventChannel")
private val MethodCall = ClassName("io.flutter.plugin.common", "MethodCall")
private val MethodChannelResult = ClassName("io.flutter.plugin.common.MethodChannel", "Result")
private val toEventStreamHandler = MemberName(flutterKmpPackageName, "toEventStreamHandler")
