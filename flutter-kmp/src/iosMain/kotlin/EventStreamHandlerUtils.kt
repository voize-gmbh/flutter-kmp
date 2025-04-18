package de.voize.flutterkmp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer
import platform.darwin.NSObject
import flutter.FlutterStreamHandlerProtocol
import flutter.FlutterEventSink
import flutter.FlutterError

typealias CreateFlutterError = (code: String, message: String?, details: Any?) -> FlutterError

inline fun <reified T> Flow<T>.toEventStreamHandler(noinline createFlutterError: CreateFlutterError): NSObject =
    toEventStreamHandler(serializer<T>(), createFlutterError)

fun <T> Flow<T>.toEventStreamHandler(
    serializer: SerializationStrategy<T>,
    createFlutterError: CreateFlutterError,
): NSObject {
    return object : FlutterStreamHandlerProtocol, NSObject() {
        var job: Job? = null

        override fun onListenWithArguments(
            arguments: Any?,
            eventSink: FlutterEventSink,
        ): FlutterError? {
            if (eventSink == null) {
                return createFlutterError("no_event_sink", "No event sink available", null)
            }

            job = CoroutineScope(Dispatchers.Default).launch {
                this@toEventStreamHandler.collect {
                    withContext(Dispatchers.Main) {
                        eventSink(flowToFlutterJson.encodeToString(serializer, it))
                    }
                }
            }
            return null
        }

        override fun onCancelWithArguments(arguments: Any?): FlutterError? {
            job?.cancel()
            job = null
            return null
        }
    }
}
