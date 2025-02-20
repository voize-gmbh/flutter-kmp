package de.voize.flutterkmp.ksp.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.*

class DartGenerator(private val codeGenerator: CodeGenerator) {

    /**
     * For a flow property annotated with @FlutterFlow we generate a property
     * that contains the broadcast stream on the corresponding channel.
     *
     * This makes sure that `receiveBroadcastStream` is only called once on the event channel.
     * The broadcast stream is equivalent to a SharedFlow, where multiple subscribers can listen to the same stream
     * and subscribers are only notified of events that are emitted after they have subscribed.
     *
     * The underlying flow is only launched when someone listens to the stream making the statement lightweight.
     *
     * Generated code:
     * ```dart
     * final Stream<bool> myFlow = const EventChannel('MyModule_myFlow')
     *    .receiveBroadcastStream()
     *    .map((event) => event as bool);
     * ```
     */
    private fun KSPropertyDeclaration.generateFlowMember(moduleName: String): String {
        val propertyName = simpleName.asString()
        val channelName = "${moduleName}_$propertyName"

        require(type.resolve().declaration.qualifiedName?.asString() == "kotlinx.coroutines.flow.Flow") {
            "Flow methods must return a Flow"
        }

        val flowTypeArgument =  type.resolve().resolveTypeArgument(0).toDartType()

        return """
final Stream<${flowTypeArgument.toTypeName()}> $propertyName = const EventChannel('$channelName')
    .receiveBroadcastStream()
    .map((event) => ${flowTypeArgument.getDartDeserializationStatement("event", deserializePrimitive = true)});
""".trimIndent()
    }



    /**
     * For a flow property annotated with @FlutterStateFlow we generate a method
     * that sets up a stream controller that emits the latest value of the flow.
     *
     * Generated code:
     * ```dart
     * StreamSubscription<bool?> myFlow(Function(bool?) onData) {
     *     final streamController = StreamController<int>();
     *     final subscriptionId = streamController.hashCode.toString();
     *
     *     Future<bool?> next(bool? previous) async {
     *          return await methodChannelToNative.invokeMethod<int>(
     *              'myFlow',
     *              [subscriptionId, previous]
     *          );
     *     }
     *
     *     void startEmittingValues() async {
     *       int? currentValue;
     *       while (!streamController.isClosed) {
     *         try {
     *           currentValue = await next(currentValue);
     *           if (!streamController.isClosed) {
     *             streamController.add(currentValue);
     *           }
     *         } catch (e) {
     *           if (!streamController.isClosed) {
     *             streamController.addError(e);
     *           }
     *         }
     *       }
     *     }
     *
     *     streamController.onListen = startEmittingValues;
     *
     *     return streamController.stream.listen(onData);
     * }
     */
    private fun KSDeclaration.generateStateFlowMethod(): String {
        val propertyName = simpleName.asString()
        val channelName = propertyName
        val flowTypeArgument = getStateFlowDeclarationFlowTypeArgument().toDartType()

        val parameters = when (this) {
            is KSPropertyDeclaration -> emptyList()
            is KSFunctionDeclaration -> parameters
            else -> error("only property and function declaration allowed for @FlutterStateFlow")
        }

        val dartParameters = parameters.map {
            (it.name?.asString() ?: error("parameter has no name")) to it.type.resolve().toDartType()
        }

        val parameterStrings = dartParameters.map { (name, dartType) ->
            "${dartType.toTypeName()} $name"
        } + listOf("Function(${flowTypeArgument.nullable().toTypeName()}) onData")

        val serializationStatements = dartParameters.mapNotNull { (name, dartType) ->
            dartType.getDartSerializationStatement(name)
        }

        val intermediateType = if (flowTypeArgument.requiresSerialization()) {
            DartType.Primitive.String()
        } else flowTypeArgument

        val invokeMethodArguments = listOf("previous") + dartParameters.map { (varName, type) ->
            if (type.requiresSerialization()) {
                "${varName}Serialized"
            } else {
                varName
            }
        }

        return """
        StreamSubscription<${flowTypeArgument.nullable().toTypeName()}> $propertyName(${parameterStrings.joinToString(", ")}) {
    final streamController = StreamController<${flowTypeArgument.nullable().toTypeName()}>();
    ${serializationStatements.joinToString("\n")}

    Future<${intermediateType.nullable().toTypeName()}> next(${intermediateType.nullable().toTypeName()} previous) async {
    return await methodChannelToNative.invokeMethod<${intermediateType.toTypeName()}>(
            '$channelName',
            [${invokeMethodArguments.joinToString(", ")}]
        );
    }
    
    void startEmittingValues() async {
        ${intermediateType.nullable().toTypeName()} currentValue;
        while (!streamController.isClosed) {
            try {
                currentValue = await next(currentValue);
                if (!streamController.isClosed) {
                    if (currentValue == null) {
                        streamController.add(null);
                    } else {
                        streamController.add(${flowTypeArgument.getDartDeserializationStatement("currentValue")});
                    }
                }
            } catch (e) {
                if (!streamController.isClosed) {
                    streamController.addError(e);
                }
            }
        }
    }
    
    streamController.onListen = startEmittingValues;
    
    return streamController.stream.listen(onData);
}
""".trimIndent()
    }


    private fun KSFunctionDeclaration.generateMethod(): String {
        val methodName = simpleName.asString()
        val eventName = simpleName.asString()
        val returnTypeNotNull = returnType?.resolve() ?: error("return type is not set")
        val dartParameters = parameters.map {
            (it.name?.asString() ?: error("parameter has no name")) to it.type.resolve().toDartType()
        }

        val dartParameterListString = dartParameters.joinToString(", ") { (name, type) ->
            "${type.toTypeName()} $name"
        }

        val serializationStatements = dartParameters.mapNotNull { (varName, type) ->
            type.getDartSerializationStatement(varName)
        }

        val dartArguments = dartParameters.joinToString(", ") { (varName, type) ->
            if (type.requiresSerialization()) {
                "${varName}Serialized"
            } else {
                varName
            }
        }

        return if (returnsUnit()) {
"""
Future<void> ${methodName}($dartParameterListString) async {
    ${serializationStatements.joinToString("\n")}
    await methodChannelToNative.invokeMethod<void>('${eventName}', [$dartArguments]);
}
""".trimIndent()
        } else {
            val invokeResultVarName = "invokeResult"
            val resultVarName = "result"
            val returnType = returnTypeNotNull.toDartType()
            val returnTypeName = returnType.toTypeName()
            val invokeReturnTypeName = if (returnType.requiresSerialization()) {
                "String"
            } else returnType.toTypeName()

            if (returnTypeNotNull.isMarkedNullable) {
"""
Future<$returnTypeName> ${methodName}($dartParameterListString) async {
    ${serializationStatements.joinToString("\n")}
    final $invokeResultVarName = await methodChannelToNative.invokeMethod<$invokeReturnTypeName>(
        '${eventName}',
        [$dartArguments],
    );
    final $resultVarName = ${returnType.getDartDeserializationStatement(invokeResultVarName)};
    return $resultVarName;
}
""".trimIndent()
            } else {
"""
Future<$returnTypeName> ${methodName}($dartParameterListString) async {
    ${serializationStatements.joinToString("\n")}
    final $invokeResultVarName = await methodChannelToNative.invokeMethod<$invokeReturnTypeName>(
        '${eventName}',
        [$dartArguments],
    );

    if ($invokeResultVarName == null) {
        throw PlatformException(code: '1', message: 'Method $methodName failed');
    }

    final $resultVarName = ${returnType.getDartDeserializationStatement(invokeResultVarName)};

    return $resultVarName;
}
""".trimIndent()
            }
        }
    }

    private fun generateModule(module: FlutterKMPSymbolProcessor.FlutterModule) {
        // TODO Only include dart:convert when needed
        val moduleString = """
// Generated by flutter-kmp. Do not modify.

import 'dart:async';
import 'dart:convert';
import 'package:iso_duration/iso_duration.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'models.dart';
               
class ${module.moduleName} {
  final methodChannelToNative = const MethodChannel("${module.moduleName}");
  
  ${module.flutterFlows.joinToString("\n") { it.generateFlowMember(module.moduleName) }}
  ${module.flutterStateFlows.joinToString("\n") { it.generateStateFlowMethod() }}
  ${module.flutterMethods.joinToString("\n") { it.generateMethod() }}
}
""".trimIndent()


        val stream = codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            "flutterkmp",
            module.moduleName,
            extensionName = "dart",
        )

        stream.write(moduleString.toByteArray())
        stream.close()
    }

    fun generate(flutterModules: List<FlutterKMPSymbolProcessor.FlutterModule>) {
        generateModels(flutterModules)
        flutterModules.forEach { module ->
            generateModule(module)
        }
    }

    private fun generateModels(flutterModules: List<FlutterKMPSymbolProcessor.FlutterModule>) {
        val (types, flutterModulesOriginatingFiles) = typesFrom(flutterModules)
        val customTypes = filterTypesForGeneration(findAllUsedTypes(types))

        val dartTypeDeclarations = customTypes.map { type ->
            createDartTypeDeclaration(type)
        }

        val stream = codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            "flutterkmp",
            "models",
            extensionName = "dart",
        )

        stream.write("import 'package:json_annotation/json_annotation.dart';\n".toByteArray())
        stream.write("import 'package:flutter/material.dart';\n\n".toByteArray())
        stream.write("import 'package:iso_duration/iso_duration.dart';\n\n".toByteArray())
        stream.write("part 'models.g.dart';\n\n".toByteArray())
        stream.write(dartTypeDeclarations.joinToString("\n").toByteArray())
        stream.close()
    }

    private fun KSClassDeclaration.generateDartClass(): String {
        require(allSuperclassesSealed()) {
            "inheritance other a sealed superclass are now allowed: $this"
        }

        val superClassName = getSealedSuperclass()?.getClassNameWithParentPrefix()
        val className = getClassNameWithParentPrefix()

        val (overwriteProperties, properties) = getAllProperties().partition {
            it.modifiers.contains(Modifier.OVERRIDE)
        }

        val constructorParams = overwriteProperties.map { property ->
            "super.${property.simpleName.asString()}"
        } + properties.map { property ->
            "this.${property.simpleName.asString()}"
        }

        // `explicitToJson: true` enables `toJson` calls nested objects
        return """
@JsonSerializable(explicitToJson: true)
class $className${superClassName?.let { " extends $superClassName" } ?: ""} {
${generateClassPropertiesAndConstructor()}

factory $className.fromJson(Map<String, dynamic> json) => _${'$'}${className}FromJson(json);

Map<String, dynamic> toJson() => _${'$'}${className}ToJson(this);
}
        """.trimIndent()
    }

    private fun KSClassDeclaration.generateClassPropertiesAndConstructor(): String {
        val className = getClassNameWithParentPrefix()
        val (overwriteProperties, properties) = getAllProperties().partition {
            it.modifiers.contains(Modifier.OVERRIDE)
        }

        val constructorParams = overwriteProperties.map { property ->
            "super.${property.simpleName.asString()}"
        } + properties.map { property ->
            "this.${property.simpleName.asString()}"
        }

        return """
${properties.joinToString("\n") { property ->
    val dartType = property.type.resolve().toDartType()
    val propName = property.simpleName.asString()
    val requiresCustomSerialization = property.requiresCustomSerializationInDart()

    buildString {
        if (requiresCustomSerialization) {
            appendLine("@JsonKey(toJson: _${propName}ToJson, fromJson: _${propName}FromJson)")
        }
        appendLine("final ${dartType.toTypeName()} ${propName};")
    }
}}

${className}(${constructorParams.joinToString(", ")});

${properties.filter { it.requiresCustomSerializationInDart() }.joinToString("\n") { property ->
    property.generateCustomSerialization()
}}
        """.trimIndent()
    }

    private fun KSPropertyDeclaration.generateCustomSerialization(): String {
        val customSerialization = findCustomSerialization()
        val propName = simpleName.asString()
        val type = type.resolve()
        val dartType = type.toDartType()
        val jsonType = (if (type.declaration.isListOrSet()) {
            "List<dynamic>"
        } else if (type.declaration.isMap()) {
            "Map<String, dynamic>"
        } else {
            customSerialization.serializedType
        }) + if (dartType.isNullable) "?" else ""

        fun generateNestedStatement(type: KSType, varName: String, isFromJson: Boolean): String {
            return (if (type.isMarkedNullable) {
                "$varName == null ? null : "
            } else { "" }) + if (type.declaration.isListOrSet()) {
                val arg = type.resolveTypeArgument(0)
                "${if (isFromJson) { "($varName as List<dynamic>)"} else { varName }}.map((e) => ${generateNestedStatement(arg, "e", isFromJson)}).toList()"
            } else if (type.declaration.isMap()) {
                val keyType = type.resolveTypeArgument(0)
                val keyTypeName = keyType.declaration.qualifiedName?.asString()
                val valueType = type.resolveTypeArgument(1)

                require(keyTypeName == "kotlin.String") {
                    "Key type of map can only be String but was $keyTypeName"
                }

                "($varName as Map<String, dynamic>).map((k, e) => MapEntry(k, ${generateNestedStatement(valueType, "e", isFromJson)}))"
            } else {
                 if (isFromJson) {
                    customSerialization.deserializeFnString(varName, type)
                } else {
                    customSerialization.serializeFnString(varName, type)
                }
            }
        }

        return """
        static $jsonType _${propName}ToJson(${dartType.toTypeName()} obj) => ${generateNestedStatement(type, "obj", false)};
        static ${dartType.toTypeName()} _${propName}FromJson($jsonType json) => ${generateNestedStatement(type, "json", true)};
        """
    }

    private fun createDartTypeDeclaration(
        declaration: KSDeclaration,
    ): String {
        return when (declaration) {
            is KSClassDeclaration -> {
                val className = declaration.getClassNameWithParentPrefix()

                when (declaration.classKind) {
                    ClassKind.CLASS -> {

                        if (Modifier.DATA in declaration.modifiers) {
                            // data class
                            declaration.assertSerializable {
                                "Data classes must be annotated with @Serializable: $declaration"
                            }
                            declaration.generateDartClass()
                        } else if (Modifier.SEALED in declaration.modifiers) {
                            declaration.assertSerializable {
                                "Sealed classes must be annotated with @Serializable: $declaration"
                            }
                            val superClassName = declaration.getSealedSuperclass()?.getClassNameWithParentPrefix()
                            val subclasses = declaration.getSealedSubclasses()
                            require(subclasses.toList().isNotEmpty()) {
                                "Sealed class must have at least one subclass"
                            }

                            val subclassesToDiscriminator =
                                subclasses.associateWith { it.getSealedSubclassDiscriminatorValue() }
                            val discriminatorKey =
                                declaration.getDiscriminatorKeyForSealedClass()


                            """
sealed class $className${superClassName?.let { " extends $superClassName" } ?: ""} {
${declaration.generateClassPropertiesAndConstructor()}

factory $className.fromJson(Map<String, dynamic> json) {
    final discriminator = json['$discriminatorKey'] as String;

    switch (discriminator) {
    ${subclassesToDiscriminator.entries.joinToString("\n") { (subclass, name) ->
        """
case '$name':
    return ${subclass.getClassNameWithParentPrefix()}.fromJson(json);
        """.trimIndent()}}
        default:
            throw Exception('Unknown class: ${'$'}discriminator');
    }
}

static Map<String, dynamic> toJson($className obj) {
    switch (obj.runtimeType) {
    ${subclassesToDiscriminator.entries.joinToString("\n") { (subclass, name) ->
        """
case ${subclass.getClassNameWithParentPrefix()}:
	final data = (obj as ${subclass.getClassNameWithParentPrefix()}).toJson();
    data['type'] = "$name";
    return data;
        """.trimIndent()}}
        default:
            throw Exception('Unknown class: ${'$'}obj');
    }
}
                            }
                            """.trimIndent()
                        } else {
                            error("Only data classes and sealed classes are supported, found: $declaration")
                        }
                    }

                    ClassKind.ENUM_CLASS -> {
                        declaration.assertSerializable {
                            "Enum class must be annotated with @Serializable: $declaration"
                        }

                        return """
enum $className {
${declaration.declarations.filterIsInstance<KSClassDeclaration>()
    .filter { it.classKind == ClassKind.ENUM_ENTRY }
    .joinToString(", ") { enumEntry ->
        enumEntry.simpleName.asString()
    }}
}
""".trimIndent()
                    }

                    ClassKind.OBJECT -> {
                        declaration.assertSerializable {
                            "Object must be annotated with @Serializable: $declaration"
                        }

                        require(declaration.getAllProperties().toList().isEmpty()) {
                            "Only objects without properties are supported"
                        }

                        declaration.generateDartClass()
                    }

                    ClassKind.INTERFACE -> error("Interfaces are not supported")
                    ClassKind.ENUM_ENTRY -> error("Enum entries are not supported")
                    ClassKind.ANNOTATION_CLASS -> error("Annotation classes are not supported")
                }
            }

            is KSFunctionDeclaration -> error("Function declarations are not supported")
            is KSTypeAlias -> TODO("type alias")
            is KSPropertyDeclaration -> error("Property declarations are not supported")
            is KSTypeParameter -> error("Type parameter declarations are not supported")
            else -> error("Unsupported declaration: $declaration")
        }
    }

    data class CustomSerialization(
        val detect: (prop: KSPropertyDeclaration) -> Boolean,
        val serializeFnString: (varName: String, type: KSType) -> String,
        val deserializeFnString: (varName: String, type: KSType) -> String,
        val serializedType: String,
    )

    val customSerializations = listOf(
        // TimeOfDay is part of the material package and is not serializable by default
        CustomSerialization(
            detect = { prop ->
                prop.type.resolve().checkNested {
                    this.toDartType() is DartType.TimeOfDay
                }
             },
            serializeFnString = { varName, _ -> "\"${'$'}{$varName.hour.toString().padLeft(2, '0')}:${'$'}{$varName.minute.toString().padLeft(2, '0')}\"" },
            deserializeFnString = { varName, _ -> "TimeOfDay.fromDateTime(DateTime.parse(\"1998-01-01T${'$'}$varName:00.000\"))" },
            serializedType = "String",
        ),
        CustomSerialization(
            detect = { prop ->
                prop.type.resolve().checkNested {
                    this.toDartType() is DartType.LocalDate
                }
            },
            serializeFnString = { varName, _ -> "$varName.toIso8601String().split('T').first" },
            deserializeFnString = { varName, _ -> "DateTime.parse($varName)" },
            serializedType = "String",
        ),
        CustomSerialization(
            detect = { prop ->
                prop.type.resolve().checkNested {
                    this.toDartType() is DartType.Duration
                }
            },
            serializeFnString = { varName, _ -> "$varName.toIso8601String()" },
            deserializeFnString = { varName, _ -> "parseIso8601Duration($varName)" },
            serializedType = "String",
        ),
        CustomSerialization(
            detect = { prop ->
                prop.type.resolve().checkNested {
                    declaration.modifiers.contains(Modifier.SEALED)
                }
            },
            serializeFnString = { varName, type ->
                "${type.toDartType().nullable(false).toTypeName()}.toJson($varName)"
            },
            deserializeFnString = { varName, type ->
                "${type.toDartType().nullable(false).toTypeName()}.fromJson($varName)"
            },
            serializedType = "Map<String, dynamic>",
        )
    )

    private fun KSPropertyDeclaration.findCustomSerialization(): CustomSerialization {
        return customSerializations.find {
            it.detect(this)
        } ?: error("no custom serialization found for property $this")
    }

    private fun KSPropertyDeclaration.requiresCustomSerializationInDart(): Boolean {
        return customSerializations.any {it.detect(this) }
    }
}
