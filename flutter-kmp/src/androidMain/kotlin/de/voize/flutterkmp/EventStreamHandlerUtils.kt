package de.voize.flutterkmp

import io.flutter.plugin.common.EventChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer

inline fun <reified T> Flow<T>.toEventStreamHandler(): EventChannel.StreamHandler =
    toEventStreamHandler(serializer<T>())

fun <T> Flow<T>.toEventStreamHandler(serializer: SerializationStrategy<T>): EventChannel.StreamHandler {
    return object : EventChannel.StreamHandler {
        // Scope lives for exactly one listen↔cancel cycle (mirror of the iOS handler):
        // created in onListen, cancelled in onCancel and on a re-listen, so it is never leaked.
        private var scope: CoroutineScope? = null

        override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
            if (events == null) return

            // Defensive: if Flutter re-listens without cancelling first, tear down the
            // previous subscription so the old coroutine/scope is not orphaned.
            scope?.cancel()
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            this.scope = scope
            scope.launch {
                this@toEventStreamHandler
                    // Encode upstream so a serialization failure is also routed through catch.
                    .map { flowToFlutterJson.encodeToString(serializer, it) }
                    .catch { error ->
                        // Only report recoverable exceptions. Errors (OutOfMemoryError,
                        // StackOverflowError, …) are semi-unrecoverable and are rethrown so they
                        // are not masked by being forwarded to Dart.
                        if (error !is Exception) throw error
                        // Surface a Flow / serialization error to Dart's stream onError instead
                        // of letting the coroutine fail silently. (catch is cancellation-
                        // transparent: a normal onCancel does not reach this branch.)
                        withContext(Dispatchers.Main) {
                            if (isActive) {
                                events.error(
                                    "flow_error",
                                    error.message ?: "Flow collection failed",
                                    null,
                                )
                            }
                        }
                    }
                    .collect { encoded ->
                        withContext(Dispatchers.Main) {
                            // Don't deliver to the sink after cancellation.
                            if (isActive) events.success(encoded)
                        }
                    }
            }
        }

        override fun onCancel(arguments: Any?) {
            scope?.cancel()
            scope = null
        }
    }
}