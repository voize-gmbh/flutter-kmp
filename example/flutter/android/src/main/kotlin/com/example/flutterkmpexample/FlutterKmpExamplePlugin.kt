package com.example.flutterkmpexample

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodChannel

class FlutterKmpExamplePlugin: FlutterPlugin {
  private val methodChannels = mutableListOf<MethodChannel>()

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    methodChannels += MyTestClassAndroid(flutterPluginBinding.binaryMessenger, SharedCoroutineScope)
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    methodChannels.forEach { it.setMethodCallHandler(null) }
  }
}