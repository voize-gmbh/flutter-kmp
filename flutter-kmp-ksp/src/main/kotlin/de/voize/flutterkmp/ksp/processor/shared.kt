package de.voize.flutterkmp.ksp.processor

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ksp.toTypeName

internal fun CodeBlock.Builder.addMethodCodeBlock(
    method: KSFunctionDeclaration,
    wrappedModuleVarName: String,
    resultStatement: (resultParameter: String) -> String = { "result.success($it)" },
    append: CodeBlock.Builder.() -> Unit = {},
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
        getKotlinSerialization(
            returnTypeDeclaration,
            "resultData",
            "serializedResultData",
            "json"
        )
        addStatement(resultStatement("serializedResultData"))
    }

    fun unitResultSuccessStatement() {
        addStatement(resultStatement("null"))
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

    append()

    endControlFlow()
}

internal fun CodeBlock.Builder.getKotlinSerialization(
    declaration: KSDeclaration,
    varName: String,
    outVarName: String,
    jsonInstanceVarName: String? = null,
) {
    if (declaration.requiresSerialization()) {
        require(jsonInstanceVarName != null)
        addStatement(
            "val %L = %L.%M(%L)",
            outVarName,
            jsonInstanceVarName,
            encodeToString,
            varName,
        )
    } else when (declaration.qualifiedName?.asString()) {
        "kotlinx.datetime.Instant",
        "kotlinx.datetime.LocalDateTime",
        "kotlinx.datetime.LocalTime",
        "kotlinx.datetime.LocalDate" -> addStatement("val %L = %L.toString()", outVarName, varName)
        "kotlin.time.Duration" -> addStatement("val %L = %L.toIsoString()", outVarName, varName)
        else -> addStatement("val %L = %L", outVarName, varName)
    }
}

internal fun CodeBlock.Builder.getKotlinDeserialization(type: KSType, varName: String, assignTo: String) {
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

internal fun KSValueParameter.toParameterSpec(): ParameterSpec {
    return ParameterSpec.builder(
        this.name?.asString() ?: error("Parameter must have a name"),
        this.type.toTypeName()
    )
        .build()
}

internal val Duration = ClassName("kotlin.time", "Duration")
internal val Instant = ClassName("kotlinx.datetime", "Instant")
internal val LocalDate = ClassName("kotlinx.datetime", "LocalDate")
internal val LocalTime = ClassName("kotlinx.datetime", "LocalTime")
internal val LocalDateTime = ClassName("kotlinx.datetime", "LocalDateTime")
internal val Dispatchers = ClassName("kotlinx.coroutines", "Dispatchers")
internal val launch = MemberName("kotlinx.coroutines", "launch")
internal val CoroutineScope = ClassName("kotlinx.coroutines", "CoroutineScope")
internal val ListOfMember = MemberName("kotlin.collections", "listOf")
internal val JsonClassName = ClassName("kotlinx.serialization.json", "Json")
internal val encodeToString = MemberName("kotlinx.serialization", "encodeToString")
internal val first = MemberName("kotlinx.coroutines.flow", "first")