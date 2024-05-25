package io.flutter.plugin.common

class MethodChannel(
    messenger: BinaryMessenger,
    name: String,
) {
    interface Result {
        fun success(result: Any?)
        fun error(errorCode: String, errorMessage: String?, errorDetails: Any?)
        fun notImplemented()
    }
    
    interface MethodCallHandler {
        fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
            error("not implemented")
        }
    }
    
    fun setMethodCallHandler(handler: MethodCallHandler) {
        error("not implemented")
    }    
}