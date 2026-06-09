package de.voize.flutterkmp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer
import platform.darwin.NSObject
import flutter.FlutterStreamHandlerProtocol
import flutter.FlutterEventSink
import objcnames.classes.FlutterError

typealias CreateFlutterError = (code: String, message: String?, details: Any?) -> FlutterError

inline fun <reified T> Flow<T>.toEventStreamHandler(noinline createFlutterError: CreateFlutterError): NSObject =
    toEventStreamHandler(serializer<T>(), createFlutterError)

fun <T> Flow<T>.toEventStreamHandler(
    serializer: SerializationStrategy<T>,
    createFlutterError: CreateFlutterError,
): NSObject {
    return object : FlutterStreamHandlerProtocol, NSObject() {
        // The scope lives for exactly one listen↔cancel cycle: it is created in onListen
        // and cancelled in onCancel (and on a re-listen). FlutterStreamHandler has no
        // separate dispose hook, so binding the scope to the subscription lifecycle is what
        // guarantees it is always closed and never leaked.
        private var scope: CoroutineScope? = null

        override fun onListenWithArguments(
            arguments: Any?,
            eventSink: FlutterEventSink,
        ): FlutterError? {
            if (eventSink == null) {
                return createFlutterError("no_event_sink", "No event sink available", null)
            }

            // Defensive: if Flutter re-listens without cancelling first, tear down the
            // previous subscription so the old coroutine/scope is not orphaned.
            scope?.cancel()
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            this.scope = scope
            scope.launch {
                this@toEventStreamHandler
                    // Encode upstream (off the main thread) so that a serialization failure is
                    // also routed through catch below, not just errors from the source Flow.
                    .map { flowToFlutterJson.encodeToString(serializer, it) }
                    .catch { error ->
                        // Only report recoverable exceptions. Errors (OutOfMemoryError,
                        // StackOverflowError, …) are semi-unrecoverable and are rethrown so they
                        // are not masked by being forwarded to Dart.
                        if (error !is Exception) throw error
                        // Surface a Flow / serialization error to Dart's stream onError instead
                        // of letting the coroutine fail silently. The FlutterError is built via
                        // the injected Swift factory, so the opaque cinterop type is never
                        // referenced here. (catch is cancellation-transparent: a normal onCancel
                        // does not reach this branch.)
                        withContext(Dispatchers.Main) {
                            if (isActive) {
                                eventSink(
                                    createFlutterError(
                                        "flow_error",
                                        error.message ?: "Flow collection failed",
                                        null,
                                    )
                                )
                            }
                        }
                    }
                    .collect { encoded ->
                        withContext(Dispatchers.Main) {
                            // Never call the sink after cancellation (Flutter forbids it).
                            if (isActive) eventSink(encoded)
                        }
                    }
            }
            return null
        }

        override fun onCancelWithArguments(arguments: Any?): FlutterError? {
            scope?.cancel()
            scope = null
            return null
        }
    }
}
