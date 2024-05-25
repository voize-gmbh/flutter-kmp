package io.flutter.plugin.common;

class EventChannel(
    messenger: BinaryMessenger,
    name: String,
) {
    interface StreamHandler {
        fun onListen(arguments: Any?, events: EventSink?)
        fun onCancel(arguments: Any?)
    }

    interface EventSink {
        fun success(event: Any?)
        fun error(errorCode: String?, errorMessage: String?, errorDetails: Any?)
        fun endOfStream()
    }

    fun setStreamHandler(handler: EventChannel.StreamHandler?) {
        error("not implemented")
    }
}
