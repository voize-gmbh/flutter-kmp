package de.voize.flutterkmp.ksp.processor

import com.google.devtools.ksp.symbol.*

internal sealed class DartType {
    abstract fun toTypeName(): String
    abstract val isNullable: Boolean
    abstract fun nullable(isNullable: Boolean = true): DartType

    protected fun markIfNullable(): String {
        return if (isNullable) "?" else ""
    }

    data class Primitive(
        val typeName: String,
        override val isNullable: Boolean = false,
    ) : DartType() {
        override fun nullable(isNullable: Boolean) =
            this.copy(isNullable = isNullable)
        override fun toTypeName() = typeName + markIfNullable()
    }

    data class DateTime(
        override val isNullable: Boolean = false,
    ) : DartType() {
        override fun nullable(isNullable: Boolean) =
        this.copy(isNullable = isNullable)
        override fun toTypeName() = "DateTime" + markIfNullable()
    }

    data class LocalDate(
        override val isNullable: Boolean = false,
    ) : DartType() {
        override fun nullable(isNullable: Boolean) =
            this.copy(isNullable = isNullable)
        override fun toTypeName() = "DateTime" + markIfNullable()
    }

    data class TimeOfDay(
        override val isNullable: Boolean = false,
    ) : DartType() {
        override fun nullable(isNullable: Boolean) =
        this.copy(isNullable = isNullable)
        override fun toTypeName() = "TimeOfDay" + markIfNullable()
    }

    data class LocalDateTime(
        override val isNullable: Boolean = false,
    ) : DartType() {
        override fun nullable(isNullable: Boolean) =
        this.copy(isNullable = isNullable)
        override fun toTypeName() = "DateTime" + markIfNullable()
    }

    data class Duration(
        override val isNullable: Boolean = false,
    ) : DartType() {
        override fun nullable(isNullable: Boolean) =  this.copy(isNullable = isNullable)
        override fun toTypeName() = "Duration" + markIfNullable()
    }

    data class Class(
        val name: String,
        val declaration: KSClassDeclaration,
        override val isNullable: Boolean = false,
    ) : DartType() {
        override fun nullable(isNullable: Boolean) = this.copy(isNullable = isNullable)
        override fun toTypeName() = name + markIfNullable()
    }
    data class List(
        val type: DartType,
        override val isNullable: Boolean = false,
    ) : DartType() {
        override fun nullable(isNullable: Boolean) = this.copy(isNullable = isNullable)
        override fun toTypeName() =
            "List<${type.toTypeName()}>" + markIfNullable()
    }
    data class Map(
        val keyType: DartType,
        val valueType: DartType,
        override val isNullable: Boolean = false,
    ) : DartType() {
        override fun nullable(isNullable: Boolean) = this.copy(isNullable = isNullable)
        override fun toTypeName() =
            "Map<${keyType.toTypeName()}, ${valueType.toTypeName()}>" + markIfNullable()
    }
}

internal fun DartType.requiresSerialization(): Boolean {
    return when (this) {
        is DartType.Primitive -> false
        is DartType.Class,
        is DartType.List,
        is DartType.Map,
        is DartType.DateTime,
        is DartType.LocalDate,
        is DartType.TimeOfDay,
        is DartType.LocalDateTime,
        is DartType.Duration -> true
    }
}

internal fun KSType.resolveTypeArgument(index: Int): KSType {
    val argument = arguments[index]
    val type = argument.type ?: error("Could not resolve type argument")
    return type.resolve()
}

internal fun KSType.toDartType(): DartType {
    return when (this.declaration.qualifiedName?.asString()) {
        "kotlin.String" -> DartType.Primitive("String", this.isMarkedNullable)
        "kotlin.Int" -> DartType.Primitive("int", this.isMarkedNullable)
        "kotlin.Float" -> DartType.Primitive("double", this.isMarkedNullable)
        "kotlin.Double" -> DartType.Primitive("double", this.isMarkedNullable)
        "kotlin.Boolean" -> DartType.Primitive("bool", this.isMarkedNullable)
        "kotlin.Array", "kotlin.collections.List", "kotlin.collections.Set" -> {
            DartType.List(resolveTypeArgument(0).toDartType(), this.isMarkedNullable)
        }
        "kotlin.collections.Map" -> DartType.Map(
            resolveTypeArgument(0).toDartType().also {
                require(it is DartType.Primitive && it.typeName == "String") {
                    "Key type of Map can only be String"
                }
            },
            resolveTypeArgument(1).toDartType(),
            this.isMarkedNullable,
        )
        "kotlin.time.Duration" -> DartType.Duration(this.isMarkedNullable)
        "kotlinx.datetime.Instant" -> DartType.DateTime(this.isMarkedNullable)
        "kotlinx.datetime.LocalDate" -> DartType.LocalDate(this.isMarkedNullable)
        "kotlinx.datetime.LocalDateTime" -> DartType.LocalDateTime(this.isMarkedNullable)
        "kotlinx.datetime.LocalTime" -> DartType.TimeOfDay(this.isMarkedNullable)
         else -> when (val declaration = this.declaration) {
            is KSClassDeclaration -> {
                //val sealedSuperclass = declaration.getSealedSuperclass()
                when (declaration.classKind) {
                    ClassKind.INTERFACE -> error("Interfaces are not supported")
                    ClassKind.CLASS -> {
                        if (Modifier.DATA in declaration.modifiers) {
                            DartType.Class(
                                declaration.getClassNameWithParentPrefix(),
                                declaration,
                                this.isMarkedNullable,
                            )
                        } else if (Modifier.SEALED in declaration.modifiers) {
                            DartType.Class(
                                declaration.getClassNameWithParentPrefix(),
                                declaration,
                                this.isMarkedNullable,
                            )
                        } else {
                            error("Only data classes and sealed classes are supported, found: $declaration")
                        }
                    }
                    ClassKind.OBJECT -> DartType.Class(declaration.getClassNameWithParentPrefix(), declaration, this.isMarkedNullable,)
                    ClassKind.ENUM_CLASS -> DartType.Class(declaration.getClassNameWithParentPrefix(), declaration, this.isMarkedNullable)
                    ClassKind.ENUM_ENTRY -> error("Enum entries are not supported")
                    ClassKind.ANNOTATION_CLASS -> error("Annotation classes are not supported")
                }
            }

            is KSFunctionDeclaration -> error("Function declarations are not supported")
            is KSTypeAlias -> error("Type aliases are not supported")
            is KSPropertyDeclaration -> error("Property declarations are not supported")
            is KSTypeParameter -> error("Type parameter are not supported")
            else -> error("Unsupported declaration: $declaration")
        }
    }
}
