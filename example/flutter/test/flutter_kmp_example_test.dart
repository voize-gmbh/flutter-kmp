import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_kmp_example/flutter_kmp_example.dart';
import 'package:flutter_kmp_example/flutter_kmp_example_platform_interface.dart';
import 'package:flutter_kmp_example/flutter_kmp_example_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFlutterKmpExamplePlatform
    with MockPlatformInterfaceMixin
    implements FlutterKmpExamplePlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final FlutterKmpExamplePlatform initialPlatform = FlutterKmpExamplePlatform.instance;

  test('$MethodChannelFlutterKmpExample is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFlutterKmpExample>());
  });

  test('getPlatformVersion', () async {
    FlutterKmpExample flutterKmpExamplePlugin = FlutterKmpExample();
    MockFlutterKmpExamplePlatform fakePlatform = MockFlutterKmpExamplePlatform();
    FlutterKmpExamplePlatform.instance = fakePlatform;

    expect(await flutterKmpExamplePlugin.getPlatformVersion(), '42');
  });
}
