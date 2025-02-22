# CHANGELOG

## unreleased

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
