package com.example.flutterkmpexample

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodChannel

class FlutterKmpExamplePlugin: FlutterPlugin {
  private var methodChannels = emptyList<MethodChannel>()

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    methodChannels = listOf(
      MyTestModuleAndroid(flutterPluginBinding.binaryMessenger, SharedCoroutineScope),
      MySecondTestModuleAndroid(flutterPluginBinding.binaryMessenger, SharedCoroutineScope),
    )
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    methodChannels.forEach { it.setMethodCallHandler(null) }
  }
}