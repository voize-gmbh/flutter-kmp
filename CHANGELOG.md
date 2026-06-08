# CHANGELOG

## unreleased

### Added — types from compiled dependencies (`KOTLIN_LIB`)
- Support Kotlin types coming from compiled dependencies (KSP `Origin.KOTLIN_LIB`), so models from a depended-on module (via `api`/`implementation`) work across the interop boundary:
  - `requiresSerialization()` and `filterTypesForGeneration()` now handle `KOTLIN_LIB` types, restricted to the class kinds that are serializable models (data class, sealed class, enum, object). Plain stdlib classes like `kotlin.String`/`kotlin.Int` (also `KOTLIN_LIB`) are intentionally excluded, so the native side does not try to JSON-decode raw primitive values
  - stdlib base classes (e.g. `kotlin.Enum`) reached via super-type traversal and unresolved error types no longer leak into code generation

### Changed — toolchain
- Updated to Kotlin **2.3.20**, KSP **2.3.9** (new decoupled KSP2 versioning), kotlinx-coroutines/serialization **1.11.0**, Android Gradle Plugin **8.11.1**, Gradle **8.14.5**, `compileSdk` **36**.
- **Required consumer toolchain (BREAKING):** because KSP is version-locked to the Kotlin compiler, consuming projects must be on **Kotlin 2.3.20 + KSP 2.3.9**, with **AGP ≥ 8.6** and **Gradle ≥ 8.7**. The `de.voize:flutter-kmp-ksp` processor cannot be applied from a project on an older Kotlin/KSP.
- Migrated the now-removed `kotlinOptions {}` / `KotlinCompile<*>` DSL to `compilerOptions {}` / `KotlinCompilationTask<*>` (required by Kotlin 2.3.20).
- Made `scripts/version.sh` portable across BSD (macOS) and GNU (Linux) by using `perl` instead of `sed -i`.
- CI (`test.yml`) and the example are now pinned to a fixed Flutter version (3.44.1) via `subosito/flutter-action` + `.fvmrc`, instead of tracking latest stable.

### Fixed — iOS Flutter symbol contamination (KT-81937) — **BREAKING for the plugin glue**
- The Flutter Objective-C types used by the generated iOS Kotlin/Native code (`FlutterMethodChannel`, `FlutterEventChannel`, `FlutterError`) are now declared as **opaque forward declarations** (`@class`) in the cinterop stub instead of full `@interface`s, and are referenced from the `objcnames.classes` package.
  - Under Kotlin/Native 2.3.20 (`-Xccall-mode=both`, [KT-81937](https://youtrack.jetbrains.com/issue/KT-81937)) a full cinterop binding emitted hard `_OBJC_CLASS_$_Flutter…` link-time symbols into the framework. This broke linking on recent Xcode and crashed non-Flutter consumers (e.g. MAUI, where `Flutter.framework` is never loaded) at startup.
  - With the opaque declarations the framework carries **zero Flutter symbols** (`nm -u` clean) and links without `-undefined dynamic_lookup` / `-Wl,-U` and without splitting the framework. Concrete Flutter objects are created/used only on the Swift side.
- **BREAKING:** the generated iOS module `register(...)` signature changed. The `createEventChannel: (name, binaryMessenger) -> FlutterEventChannel` parameter was replaced by `setUpEventChannel: (name, binaryMessenger, handler) -> Unit` — the event channel is created **and** its stream handler set on the Swift side. Hand-written plugin entry-point Swift must be updated:
  ```swift
  // replace `createEventChannel` with:
  let setUpEventChannel = { (name: String, binaryMessenger: NSObject, handler: NSObject) in
    let eventChannel = FlutterEventChannel(name: name, binaryMessenger: binaryMessenger as! FlutterBinaryMessenger)
    guard let streamHandler = handler as? (any FlutterStreamHandler & NSObjectProtocol) else {
      fatalError("setUpEventChannel: stream handler does not conform to FlutterStreamHandler & NSObjectProtocol")
    }
    eventChannel.setStreamHandler(streamHandler)
  }
  // and pass `setUpEventChannel:` (not `createEventChannel:`) to each module's register(...)
  ```

### Added — robust error propagation (non-breaking) — iOS & Android
Exceptions thrown by wrapped Kotlin code are now reported back to Flutter as catchable errors instead of crashing the host app. **Previously every one of these was an uncaught exception in a fire-and-forget coroutine (or a Kotlin exception crossing into Objective-C on iOS) and aborted the whole process** — the Kotlin/Native framework and the Flutter engine share one process, so the entire app went down. There was no way for a consumer to intercept it, so none of the below is a breaking change: a hard crash becomes a catchable error.

- **`@FlutterFlow`**: an error while collecting is delivered to the Dart stream's `onError` as `PlatformException(code: "flow_error", message: <throwable message>)`. The `EventStreamHandler` reports it through the event sink (`createFlutterError` on iOS / `EventSink.error` on Android). The handler's coroutine scope is now bound to the listen↔cancel lifecycle (created on `onListen`, cancelled on `onCancel` and on re-listen), fixing a scope leak on re-subscription.
- **`@FlutterMethod` (suspend)**: an error is reported via the method-channel result, so the Dart `await module.method()` throws a catchable `PlatformException(code: "method_error")` instead of crashing (and instead of hanging the `Future` forever).
- **`@FlutterStateFlow`**: an error during collection/serialization is reported via the method-channel result and surfaces on the stream's `onError`. The generated Dart helper now accepts optional `onError`/`onDone` callbacks: `myState(onData, {onError, onDone})` (additive — existing `myState(onData)` calls are unchanged).
- **iOS synchronous path**: `handleMethodCall` is wrapped so that synchronous failures (argument casts, deserialization, non-suspend method bodies) are also reported as `PlatformException(code: "method_error")` rather than terminating the process. (On Android these were already converted to errors by Flutter's `MethodChannel`.)
- **Recommended:** add an `onError` handler to `.listen(...)` on `@FlutterFlow`/`@FlutterStateFlow` streams and a `try/catch` around suspend `@FlutterMethod` calls. Without an `onError`, a stream error surfaces as an unhandled Dart async error (reported to the zone / `FlutterError.onError`) rather than crashing the process.
- Note: end-of-stream for a finite `@FlutterFlow` is intentionally not signalled from Kotlin, since `FlutterEndOfEventStream` is a `Flutter.framework` symbol and referencing it would reintroduce the contamination fixed above (KT-81937).

## v0.1.0

- Update Nexus repository URLs for publishing

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
