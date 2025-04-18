# CHANGELOG

## v0.1.0-rc.6

- Fix generated serialization and deserialization for nullable class type parameters

## v0.1.0-rc.5

- Move calling constructors for FlutterMethodChannel, FlutterEventChannel and FlutterError to plugin code

## v0.1.0-rc.4

- Use cinterop with stub headers instead of Flutter cocoapod dependency

## v0.1.0-rc.3

- Fix conflicts with same method names across different modules on iOS by prefixing method names
- Update Dart duration serialization to be compatible with Kotlin ISO-8601 duration string parsing

## v0.1.0-rc.2

- Fix errors with boolean types in method return and flow types on iOS

## v0.1.0-rc.1

- Update Kotlin to 2.1.0 and KSP to 2.0.21-1.0.25
- Add support for iOS module generation
- Make generated Android modules subclass MethodChannel to enable teardown

## v0.1.0-rc.0

- Android flutter plugin module generation
