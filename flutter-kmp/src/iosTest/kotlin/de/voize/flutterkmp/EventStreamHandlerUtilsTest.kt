package de.voize.flutterkmp

import flutter.FlutterStreamHandlerProtocol
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EventStreamHandlerUtilsTest {

    // When Flutter hands a null eventSink to onListen, the handler must build a
    // FlutterError via the injected factory with code "no_event_sink".
    //
    // NOTE on the opaque cinterop limitation: FlutterError is a forward-declared (opaque)
    // ObjC type and CANNOT be constructed in a unit test (no Flutter.framework linked), so
    // the factory records the call and throws — we assert it was invoked with the right
    // code. The actual non-null FlutterError bridging back to Flutter is validated by the
    // integration run (the example app), not here.
    @Test
    fun onListen_withNullEventSink_invokesCreateFlutterErrorWithNoEventSinkCode() {
        var capturedCode: String? = null

        @Suppress("UNCHECKED_CAST")
        val handler = emptyFlow<Int>().toEventStreamHandler(
            Int.serializer(),
        ) { code, _, _ ->
            capturedCode = code
            throw RuntimeException("createFlutterError invoked")
        } as FlutterStreamHandlerProtocol

        assertFailsWith<RuntimeException> {
            handler.onListenWithArguments(null, null)
        }
        assertEquals("no_event_sink", capturedCode)
    }

    // onCancel must not throw and returns null (no FlutterError) even when no stream
    // was ever started (job is null). Defensive null handling of the internal job.
    @Test
    fun onCancel_withoutActiveStream_returnsNullAndDoesNotThrow() {
        val handler = emptyFlow<Int>().toEventStreamHandler(
            Int.serializer(),
        ) { _, _, _ -> throw AssertionError("createFlutterError should not be called on cancel") }
            as FlutterStreamHandlerProtocol

        val result = handler.onCancelWithArguments(null)
        assertEquals(null, result)
    }
}
