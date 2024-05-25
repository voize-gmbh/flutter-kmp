import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_kmp_example_method_channel.dart';

abstract class FlutterKmpExamplePlatform extends PlatformInterface {
  /// Constructs a FlutterKmpExamplePlatform.
  FlutterKmpExamplePlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterKmpExamplePlatform _instance = MethodChannelFlutterKmpExample();

  /// The default instance of [FlutterKmpExamplePlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterKmpExample].
  static FlutterKmpExamplePlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterKmpExamplePlatform] when
  /// they register themselves.
  static set instance(FlutterKmpExamplePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
