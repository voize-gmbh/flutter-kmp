package de.voize.flutterkmp.ksp.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

fun KSFunctionDeclaration.returnsUnit(): Boolean {
    return returnType?.resolve()?.declaration?.qualifiedName?.asString() == "kotlin.Unit"
}

private fun KSClassDeclaration.isSealedClassSubclass() =
    this.superTypes.any { Modifier.SEALED in it.resolve().declaration.modifiers }

fun KSClassDeclaration.allSuperclassesSealed(): Boolean {
    return this.superTypes.all {
        Modifier.SEALED in it.resolve().declaration.modifiers ||
            it.resolve().declaration.qualifiedName?.asString() == "kotlin.Any"
    }
}

fun KSClassDeclaration.getSealedSuperclass(): KSDeclaration? {
    return if (isSealedClassSubclass()) {
        superTypes.map { it.resolve().declaration }
            .single { Modifier.SEALED in it.modifiers }
    } else null
}

/**
 * Recursively get all super classes of this class and concatenate the class names.
 */
fun KSDeclaration.getClassNameWithParentPrefix(): String {
    return parentDeclaration?.let {
        "${it.getClassNameWithParentPrefix()}${simpleName.asString()}"
    } ?: simpleName.asString()
}

fun KSDeclaration.getStateFlowDeclarationFlowTypeArgument(): KSType {
    return when (this) {
        is KSPropertyDeclaration -> type.resolve()
        is KSFunctionDeclaration -> (returnType ?: error("no return type")).resolve()
        else -> error("only property and function declaration allowed for @FlutterStateFlow")
    }.also {
        require(it.declaration.qualifiedName?.asString() == "kotlinx.coroutines.flow.Flow") {
            "Flow methods must return a Flow"
        }
    }.resolveTypeArgument(0)
}

fun typesFrom(flutterModules: List<FlutterKMPSymbolProcessor.FlutterModule>): Pair<List<KSType>, List<KSFile>> {
    // Collect all types of function parameters and return types

    val typeDeclarationsFromMethods = flutterModules.flatMap { it.flutterMethods }.flatMap {
        it.parameters.map { it.type } + (it.returnType ?: error("Type resolution error"))
    }.toSet().map { it.resolve() }

    val typeDeclarationsFromFlows = flutterModules.flatMap {
        it.flutterStateFlows.map { it.getStateFlowDeclarationFlowTypeArgument() } +
        it.flutterFlows.map { it.type.resolve().resolveTypeArgument(0) }
    }

    val typeDeclarations = typeDeclarationsFromMethods + typeDeclarationsFromFlows

    val originatingKSFiles = flutterModules.mapNotNull { it.wrappedClassDeclaration.containingFile }
    return typeDeclarations to originatingKSFiles
}

/**
 * Breadth-first search to find all used types (property types, sealed types, etc), given initial types
 */
fun findAllUsedTypes(types: List<KSType>): Set<KSDeclaration> {
    val toBeProcessed = types.toMutableList()
    val processed = mutableSetOf<KSDeclaration>()

    fun scheduleForProcessing(type: KSType) {
        toBeProcessed.add(type)
    }

    while (toBeProcessed.isNotEmpty()) {
        val current = toBeProcessed.removeAt(0)
        if (current.isError) {
            continue
        }
        val declaration = current.declaration
        if (declaration !in processed) {
            processed.add(declaration)

            when (declaration) {
                is KSClassDeclaration -> {

                    when (declaration.classKind) {
                        ClassKind.CLASS -> {
                            if (com.google.devtools.ksp.symbol.Modifier.DATA in declaration.modifiers) {
                                // data class
                                declaration.getAllProperties().forEach {
                                    scheduleForProcessing(it.type.resolve())
                                }
                            } else if (com.google.devtools.ksp.symbol.Modifier.SEALED in declaration.modifiers) {
                                // sealed class
                                declaration.getSealedSubclasses().forEach {
                                    scheduleForProcessing(it.asStarProjectedType())
                                }
                            }
                        }

                        else -> Unit
                    }
                    declaration.superTypes.forEach {
                        scheduleForProcessing(it.resolve())
                    }
                }

                is KSTypeAlias -> {
                    scheduleForProcessing(declaration.type.resolve())
                }

                is KSFunctionDeclaration -> {
                    error("Function declarations are not supported")
                }

                is KSPropertyDeclaration -> {
                    scheduleForProcessing(declaration.type.resolve())
                }

                is KSTypeParameter -> {
                    declaration.bounds.map { it.resolve() }.forEach(::scheduleForProcessing)
                }

                else -> {
                    error("Unsupported declaration: $declaration")
                }
            }
        }
        current.arguments.forEach {
            val type = it.type
            // if not a type variable
            if (type != null) {
                scheduleForProcessing(type.resolve())
            }
        }
    }
    return processed
}

@OptIn(KspExperimental::class)
fun KSDeclaration.assertSerializable(message: () -> String) =
    getAnnotationsByType(Serializable::class).singleOrNull() ?: error(message)

fun KSDeclaration.isListOrSet(): Boolean {
    val types = listOf(
        "kotlin.collections.List",
        "kotlin.collections.Set",
    )
    return qualifiedName?.asString() in types
}

fun KSDeclaration.isMap(): Boolean {
    return qualifiedName?.asString() == "kotlin.collections.Map"
}

fun KSDeclaration.requiresSerialization(): Boolean {
    val types = listOf(
        "kotlin.collections.List",
        "kotlin.collections.Map",
        "kotlin.collections.Set",
        // Booleans are translated to Ints during Kotlin -> ObjC -> Dart interop (although not in the other direction),
        // leading to errors like "_TypeError (type 'int' is not a subtype of type 'bool?' in type cast)"
        // we therefore serialize them to strings.
        "kotlin.Boolean",
    )

    return qualifiedName?.asString() in types
           || (this is KSClassDeclaration && when (this.classKind) {
               ClassKind.CLASS -> this.origin == Origin.KOTLIN
               ClassKind.OBJECT -> true
               ClassKind.ENUM_CLASS -> true
               else -> false
           })
}

fun filterTypesForGeneration(types: Set<KSDeclaration>): Collection<KSDeclaration> {
    val customTypes = types.filter {
        val defaultTypes = setOf(
            "kotlin.Any",
            "kotlin.Boolean",
            "kotlin.Byte",
            "kotlin.Char",
            "kotlin.Double",
            "kotlin.Float",
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Number",
            "kotlin.Short",
            "kotlin.String",
            "kotlin.Unit",
            "kotlin.collections.List",
            "kotlin.collections.Map",
            "kotlin.collections.Set",
            "kotlin.time.Duration",
            "kotlinx.datetime.Instant",
            "kotlinx.datetime.LocalDate",
            "kotlinx.datetime.LocalDateTime",
            "kotlinx.datetime.LocalTime",
        )
        it.qualifiedName?.asString() !in defaultTypes
    }

    return customTypes.filter {
        when (it) {
            is KSClassDeclaration -> it.classKind != ClassKind.INTERFACE && it.origin == Origin.KOTLIN
            is KSTypeParameter -> false
            else -> true
        }
    }
}



fun KSClassDeclaration.getSealedSubclassDiscriminatorValue() =
    (getSerialNameAnnotationOrNull()?.value
        ?: error("Sealed subclasses must be annotated with @SerialName: $this"))

@OptIn(KspExperimental::class)
private fun KSDeclaration.getSerialNameAnnotationOrNull(): SerialName? =
    getAnnotationsByType(SerialName::class).singleOrNull()

@OptIn(KspExperimental::class, ExperimentalSerializationApi::class)
private fun KSDeclaration.getJsonClassDiscriminatorAnnotationOrNull(): JsonClassDiscriminator? =
    getAnnotationsByType(JsonClassDiscriminator::class).singleOrNull()

@OptIn(ExperimentalSerializationApi::class)
fun KSDeclaration.getDiscriminatorKeyForSealedClass(): String {
    val defaultDiscriminatorKey = "type"
    return getJsonClassDiscriminatorAnnotationOrNull()?.discriminator
        ?: defaultDiscriminatorKey
}

internal fun KSType.checkNested(condition: KSType.() -> Boolean): Boolean {
    return if (declaration.isListOrSet()) {
        resolveTypeArgument(0).checkNested(condition)
    } else if (declaration.isMap()) {
        val keyType = resolveTypeArgument(0)
        val keyTypeName = keyType.declaration.qualifiedName?.asString()
        val valueType = resolveTypeArgument(1)

        require(keyTypeName == "kotlin.String") {
            "Key type of map can only be String but was $keyTypeName"
        }

        valueType.checkNested(condition)
    } else {
        condition()
    }
}

internal fun DartType.getNestedDartDeserializationStatement(varName: String): String = when (this) {
    is DartType.Primitive -> "$varName as ${this.toTypeName()}"
    is DartType.DateTime, is DartType.LocalDateTime, is DartType.LocalDate -> "DateTime.parse($varName)"
    is DartType.Duration -> "parseIso8601Duration($varName)"
    is DartType.TimeOfDay -> "TimeOfDay.fromDateTime(\"01-01-1998T\" + DateTime.parse($varName))"
    is DartType.Class -> if (declaration.modifiers.contains(Modifier.SEALED)) {
        "${this.toTypeName()}.fromJson($varName)"
    } else if (declaration.classKind == ClassKind.ENUM_CLASS) {
        "${this.toTypeName()}.values.byName($varName)"
    } else {
        "${this.toTypeName()}.fromJson($varName)"
    }
    is DartType.List -> """
($varName as List<dynamic>).map((element) {
return ${type.getNestedDartDeserializationStatement("element")};
}).toList()
""".trimIndent()
    is DartType.Map -> """
($varName as Map<String, dynamic>).map((key, value) {
return MapEntry(key, ${valueType.getNestedDartDeserializationStatement("value")});
})
""".trimIndent()
}

internal fun DartType.getDartDeserializationStatement(varName: String, deserializePrimitive: Boolean = false): String = when (this) {
    is DartType.Primitive -> if (
        // Booleans always need deserialization
        deserializePrimitive || this is DartType.Primitive.Bool
    ) {
        "jsonDecode($varName) as ${this.toTypeName()}"
    } else varName
    is DartType.DateTime, is DartType.LocalDateTime, is DartType.LocalDate -> "DateTime.parse($varName)"
    is DartType.Duration -> "parseIso8601Duration($varName)"
    is DartType.TimeOfDay -> "TimeOfDay.fromDateTime(DateTime.parse(\"1998-01-01T${'$'}$varName:00.000\"))"
    is DartType.Class -> if (declaration.modifiers.contains(Modifier.SEALED)) {
        "${this.toTypeName()}.fromJson(jsonDecode($varName))"
    } else if (declaration.classKind == ClassKind.ENUM_CLASS) {
        "${this.toTypeName()}.values.byName(jsonDecode($varName))"
    } else {
        "${this.toTypeName()}.fromJson(jsonDecode($varName) as Map<String, dynamic>)"
    }
    is DartType.List -> """
(jsonDecode($varName) as List<dynamic>).map((element) {
return ${type.getNestedDartDeserializationStatement("element")};
}).toList()
""".trimIndent()
    is DartType.Map -> """
(jsonDecode($varName) as Map<String, dynamic>).map((key, value) {
return MapEntry(key, ${valueType.getNestedDartDeserializationStatement("value")});
})
""".trimIndent()
}

/**
* Enums can not be directly encoded like `jsonEncode(MyEnum.option1)`.
* Instead we need to encode the name of the enum like `jsonEncode(MyEnum.option1.name)`.
* This function makes sure that enums inside of Lists and Maps are transformed to their name.
*/
internal fun transformEnumsForSerialization(varName: String, type: DartType): String {
    return when (type) {
        is DartType.Primitive,
        is DartType.DateTime,
        is DartType.LocalDate,
        is DartType.TimeOfDay,
        is DartType.LocalDateTime,
        is DartType.Duration -> varName
        is DartType.Class -> if (type.declaration.classKind == ClassKind.ENUM_CLASS) {
            "${varName}.name"
        } else {
            varName
        }
        is DartType.List -> "$varName.map((e) => ${transformEnumsForSerialization("e", type.type)}).toList()"
        is DartType.Map -> "$varName.map((k, v) => MapEntry(k, ${transformEnumsForSerialization("v", type.valueType)}))"
    }
}

internal fun DartType.getDartSerializationStatement(varName: String): String? {
    return when (this) {
        is DartType.DateTime -> "if (!$varName.isUtc) throw ArgumentError('$varName must be in UTC');\n"
        is DartType.LocalDateTime -> "if ($varName.isUtc) throw ArgumentError('$varName must not be in UTC');\n"
        is DartType.LocalDate -> "if ($varName.isUtc) throw ArgumentError('$varName must not be in UTC');\n"
        else ->  ""
    } + "final ${varName}Serialized = " + when (this) {
        is DartType.Primitive -> when (this) {
            is DartType.Primitive.Bool -> "${varName}.toString()"
            is DartType.Primitive.Double,
            is DartType.Primitive.Float,
            is DartType.Primitive.Int,
            is DartType.Primitive.String -> return null
        }
        is DartType.Duration -> "${varName}.toIso8601String().$patchIso8601DurationStringDartCode"
        is DartType.TimeOfDay -> "\"${'$'}{$varName.hour.toString().padLeft(2, '0')}:${'$'}{$varName.minute.toString().padLeft(2, '0')}\""
        is DartType.LocalDate -> "${varName}.toIso8601String().split('T').first"
        is DartType.DateTime, is DartType.LocalDateTime -> "$varName.toIso8601String()"
        is DartType.Class -> {
            if (declaration.modifiers.contains(Modifier.SEALED)) {
                "jsonEncode(${toTypeName()}.toJson(${varName}))"
            } else if (declaration.classKind == ClassKind.ENUM_CLASS) {
                "jsonEncode(${varName}.name);"
            } else {
                "jsonEncode(${varName}.toJson())"
            }
        }
        is DartType.List -> "jsonEncode(${transformEnumsForSerialization(varName, this)})"
        is DartType.Map -> "jsonEncode(${transformEnumsForSerialization(varName, this)})"
    } + ";"
}

/**
 * The Pub package we use to parse Dart Durations to ISO-8601 strings, [iso_duration](https://github.com/pti/iso_duration),
 * uses W, M, Y for day designators, which are not supported in kotlin.time.Duration ISO-8601 string parsing,
 * see [here](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/-duration/-companion/parse-iso-string.html).
 *
 * This code runs as an extension on a "x.toIso8601String()" call to patch
 * the ISO-8601 duration string to use D instead of W, M, Y with W=7D, M=30D, Y=365D.
 */
internal const val patchIso8601DurationStringDartCode = """replaceFirstMapped(RegExp(r'P([^T]*)(T.*)?'), (m) {
        int totalDays = 0;
        String datePart = m[1]!
          .replaceAllMapped(RegExp(r'(\d+)Y'), (y) { totalDays += int.parse(y[1]!) * 365; return ''; })
          .replaceAllMapped(RegExp(r'(\d+)M'), (m) { totalDays += int.parse(m[1]!) * 30; return ''; })
          .replaceAllMapped(RegExp(r'(\d+)W'), (w) { totalDays += int.parse(w[1]!) * 7; return ''; })
          .replaceAllMapped(RegExp(r'(\d+)D'), (d) { totalDays += int.parse(d[1]!); return ''; });

        return 'P' + (totalDays > 0 ? '${"$"}{totalDays}D' : '') + (m[2] ?? '');
    })"""
