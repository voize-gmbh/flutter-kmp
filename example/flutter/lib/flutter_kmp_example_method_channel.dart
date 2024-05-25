import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_kmp_example_platform_interface.dart';

/// An implementation of [FlutterKmpExamplePlatform] that uses method channels.
class MethodChannelFlutterKmpExample extends FlutterKmpExamplePlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_kmp_example');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
