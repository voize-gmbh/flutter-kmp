import Flutter
import UIKit
import flutterkmpexample


public class FlutterKmpExamplePlugin: NSObject, FlutterPlugin {
  let myTestModule: MyTestModuleIOS
  let mySecondTestModule: MySecondTestModuleIOS
    
  init(myTestModule: MyTestModuleIOS, mySecondTestModule: MySecondTestModuleIOS) {
    self.myTestModule = myTestModule
    self.mySecondTestModule = mySecondTestModule
  }
    
  public static func register(with registrar: FlutterPluginRegistrar) {
    let createMethodChannel = { (name: String, binaryMessenger: NSObject) in
      FlutterMethodChannel(name: name, binaryMessenger: binaryMessenger as! FlutterBinaryMessenger)
    }
    let createEventChannel = { (name: String, binaryMessenger: NSObject) in
      FlutterEventChannel(name: name, binaryMessenger: binaryMessenger as! FlutterBinaryMessenger)
    }
    let createFlutterError = { (code: String, message: String?, details: Any?) in
      FlutterError(code: code, message: message, details: details)
    }
    let myTestModule = MyTestModuleIOS(coroutineScope: MyTestModuleKt.SharedCoroutineScope)
    let mySecondTestModule = MySecondTestModuleIOS(coroutineScope: MyTestModuleKt.SharedCoroutineScope)

    let instance = FlutterKmpExamplePlugin(
      myTestModule: myTestModule,
      mySecondTestModule: mySecondTestModule
    )

    myTestModule.register(
      registrar: registrar,
      pluginInstance: instance,
      createMethodChannel: createMethodChannel,
      createEventChannel: createEventChannel,
      createFlutterError: createFlutterError
    )

    mySecondTestModule.register(
      registrar: registrar,
      pluginInstance: instance,
      createMethodChannel: createMethodChannel,
      createEventChannel: createEventChannel,
      createFlutterError: createFlutterError
    )
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    if (self.myTestModule.handleMethodCall(call: call, result: result)) {
      return
    } else if (self.mySecondTestModule.handleMethodCall(call: call, result: result)) {
      return
    } else {
      result(FlutterMethodNotImplemented)
    }
  }
}
