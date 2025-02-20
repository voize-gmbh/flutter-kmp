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
    let myTestModule = MyTestModuleIOS(coroutineScope: MyTestModuleKt.SharedCoroutineScope)
    let mySecondTestModule = MySecondTestModuleIOS(coroutineScope: MyTestModuleKt.SharedCoroutineScope)

    let instance = FlutterKmpExamplePlugin(
      myTestModule: myTestModule,
      mySecondTestModule: mySecondTestModule
    )

    myTestModule.register(
      registrar: registrar,
      pluginInstance: instance
    )

    mySecondTestModule.register(
      registrar: registrar,
      pluginInstance: instance
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
