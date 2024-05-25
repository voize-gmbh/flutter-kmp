package de.voize.flutterkmp

import kotlinx.coroutines.flow.Flow
import io.flutter.plugin.common.EventChannel
import kotlinx.coroutines.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.serializersModuleOf
import kotlinx.serialization.serializer

private class DynamicLookupSerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Any")

    override fun serialize(encoder: Encoder, value: Any) {
        val jsonEncoder = encoder as JsonEncoder
        val jsonElement = serializeAny(value)
        jsonEncoder.encodeJsonElement(jsonElement)
    }

    private fun serializeAny(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is Map<*, *> -> {
            val mapContents = value.entries.associate { mapEntry ->
                mapEntry.key.toString() to serializeAny(mapEntry.value)
            }
            JsonObject(mapContents)
        }

        is List<*> -> {
            val arrayContents = value.map { listEntry -> serializeAny(listEntry) }
            JsonArray(arrayContents)
        }

        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        else -> error("Unsupported type ${value::class}")
    }

    override fun deserialize(decoder: Decoder): Any {
        error("Unsupported")
    }
}

private val flowToFlutterJson = Json {
    serializersModule = serializersModuleOf(DynamicLookupSerializer())
    encodeDefaults = true
}

inline fun <reified T> Flow<T>.toEventStreamHandler(): EventChannel.StreamHandler =
    toEventStreamHandler(serializer<T>())

fun <T> Flow<T>.toEventStreamHandler(serializer: SerializationStrategy<T>): EventChannel.StreamHandler {
    return object : EventChannel.StreamHandler {
        var job: Job? = null

        override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
            job = CoroutineScope(Dispatchers.Default).launch {
                this@toEventStreamHandler.collect {
                    withContext(Dispatchers.Main) {
                        events?.success(
                            flowToFlutterJson.encodeToString(serializer, it)
                        )
                    }
                }
            }
        }

        override fun onCancel(arguments: Any?) {
            job?.cancel()
            job = null
        }
    }
}