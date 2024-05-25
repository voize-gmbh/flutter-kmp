package de.voize.flutterkmp

import kotlinx.coroutines.flow.Flow
import io.flutter.plugin.common.EventChannel
import kotlinx.coroutines.*
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer

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