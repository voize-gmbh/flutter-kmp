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
import com.squareup.kotlinpoet.UNIT
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
        val setUpEventChannelArgName = "setUpEventChannel"
        val createFlutterErrorArgName = "createFlutterError"
        val methodChannelVarName = "methodChannel"

        val createFlutterErrorLambda = LambdaTypeName.get(
            parameters = listOf(
                ParameterSpec("code", STRING),
                ParameterSpec("message", STRING.copy(nullable = true)),
                ParameterSpec("details", ANY.copy(nullable = true)),
            ),
            returnType = FlutterError,
        )

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
            // Stored from register(...) so handleMethodCall can build a FlutterError to report
            // method/state-flow failures back to Flutter instead of crashing the host app.
            .addProperty(
                PropertySpec.builder(createFlutterErrorArgName, createFlutterErrorLambda)
                    .addModifiers(KModifier.PRIVATE, KModifier.LATEINIT)
                    .mutable(true)
                    .build()
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
                        setUpEventChannelArgName,
                        LambdaTypeName.get(
                            parameters = listOf(
                                ParameterSpec("name", STRING),
                                ParameterSpec("binaryMessenger", NSObject),
                                ParameterSpec("handler", NSObject),
                            ),
                            returnType = UNIT,
                        )
                    )
                    .addParameter(createFlutterErrorArgName, createFlutterErrorLambda)
                    .addCode(
                        CodeBlock.builder().apply {
                            // Keep the factory so failures surfaced from handleMethodCall can
                            // be turned into a FlutterError (the opaque type is never built here).
                            addStatement("this.%N = %N", createFlutterErrorArgName, createFlutterErrorArgName)
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
                                    "%L(%S, %L ?: error(%S), %N.%M.%M(%L))",
                                    setUpEventChannelArgName,
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
                            val iosErrorStatement = { code: String, msg: String ->
                                "result?.invoke($createFlutterErrorArgName(\"$code\", $msg, null))"
                            }
                            beginControlFlow("return try")
                            beginControlFlow("when (call.method)")
                            flutterModule.flutterMethods.forEach { method ->
                                addMethodCodeBlock(
                                    method = method,
                                    moduleName = flutterModule.moduleName,
                                    wrappedModuleVarName = wrappedModuleVarName,
                                    resultStatement = { resultParameter ->
                                        "result!!($resultParameter)"
                                    },
                                    errorStatement = iosErrorStatement,
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
                                    errorStatement = iosErrorStatement,
                                    append = { addStatement("true") }
                                )
                            }
                            addStatement("else -> false")
                            endControlFlow()
                            // Catch synchronous failures (argument casts, deserialization,
                            // non-suspend method bodies). On iOS a Kotlin exception crossing
                            // into ObjC would otherwise terminate the whole app. Errors
                            // (OOM, StackOverflow…) are intentionally left to propagate.
                            nextControlFlow("catch (e: %T)", ExceptionClassName)
                            addStatement(iosErrorStatement("method_error", "e.message"))
                            addStatement("true")
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
// NOTE (opaque cinterop, KT-81937): FlutterMethodChannel / FlutterError are declared as
// forward declarations (@class) in the cinterop stub so the generated framework emits NO
// _OBJC_CLASS_$_Flutter… link-time symbols. Kotlin/Native surfaces such forward-declared
// classes under the `objcnames.classes` package. This package name is a Kotlin/Native
// implementation detail: if a future K/N version changes it, the generated code stops
// compiling and `scripts/verify-no-flutter-symbols.sh` (run in CI) will catch a regression.
private val FlutterMethodChannel = ClassName("objcnames.classes", "FlutterMethodChannel")
private val FlutterError = ClassName("objcnames.classes", "FlutterError")
private val FlutterPluginRegistrar = ClassName("flutter", "FlutterPluginRegistrarProtocol")
private val FlutterPlugin = ClassName("flutter", "FlutterPluginProtocol")
private val toEventStreamHandler = MemberName(flutterKmpPackageName, "toEventStreamHandler")