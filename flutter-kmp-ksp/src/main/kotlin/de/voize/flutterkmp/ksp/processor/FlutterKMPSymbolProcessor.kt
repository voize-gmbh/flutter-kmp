package de.voize.flutterkmp.ksp.processor

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toTypeName

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

            val primaryConstructorParameters =
                wrappedClassDeclaration.primaryConstructor?.parameters
                    ?: emptyList()
            val constructorInvocationArguments =
                primaryConstructorParameters.map { constructorParameter ->
                    CodeBlock.of("%N", constructorParameter.name?.asString())
                }.joinToCode()
            val constructorInvocation =
                CodeBlock.of("%T(%L)", wrappedClassType, constructorInvocationArguments)

            if (JvmPlatform in platformNames && NativePlatform !in platformNames) {
                AndroidModuleGenerator().generateModule(
                    flutterModule,
                    primaryConstructorParameters,
                    constructorInvocation,
                    codeGenerator,
                )
            }
            if (JvmPlatform !in platformNames && NativePlatform in platformNames) {
                IOSKotlinModuleGenerator().generateModule(
                    flutterModule,
                    primaryConstructorParameters,
                    constructorInvocation,
                    codeGenerator,
                )
            }
        }

        if (!invoked && JvmPlatform in platformNames && NativePlatform in platformNames) {
            DartGenerator(codeGenerator, logger).generate(flutterModules)
        }

        invoked = true
        return emptyList()
    }
}

const val flutterKmpPackageName = "de.voize.flutterkmp"

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
