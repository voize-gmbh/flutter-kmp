import Flutter
import UIKit
import flutterkmpexample


public class FlutterKmpExamplePlugin: NSObject, FlutterPlugin {
  let myTestClass: MyTestClassIOS
    
  init(myTestClass: MyTestClassIOS) {
    self.myTestClass = myTestClass
  }
    
  public static func register(with registrar: FlutterPluginRegistrar) {
    let myTestClass = MyTestClassIOS(coroutineScope: MyTestClassKt.SharedCoroutineScope)
    let instance = FlutterKmpExamplePlugin(myTestClass: myTestClass)

    myTestClass.register(
      registrar: registrar,
      pluginInstance: instance
    )
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    let wasHandled = self.myTestClass.handleMethodCall(call: call, result: result)
    if (!wasHandled) {
      result(FlutterMethodNotImplemented)
    }
  }
}
