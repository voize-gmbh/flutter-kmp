
import 'flutter_kmp_example_platform_interface.dart';

class FlutterKmpExample {
  Future<String?> getPlatformVersion() {
    return FlutterKmpExamplePlatform.instance.getPlatformVersion();
  }
}
