// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/ios/framework/Headers/FlutterPlugin.h

#import <Foundation/Foundation.h>

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterChannels.h#L194
typedef void (^FlutterResult)(id _Nullable result);

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterChannels.h#L220
// Forward declaration (opaque): provided by the host app's Flutter.framework at runtime.
// Kept opaque so no _OBJC_CLASS_$_FlutterMethodChannel link-time symbol is emitted
// (Kotlin/Native 2.3.20 / KT-81937). The Kotlin side only passes it through.
@class FlutterMethodChannel;

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterChannels.h#L350
typedef void (^FlutterEventSink)(id _Nullable event);

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterCodecs.h#L246C1-L246C35
// Forward declaration (opaque): provided by the host app's Flutter.framework at runtime.
// Kept opaque so no _OBJC_CLASS_$_FlutterError link-time symbol is emitted
// (Kotlin/Native 2.3.20 / KT-81937). Created on the Swift side via a factory closure.
@class FlutterError;

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterChannels.h#L356
@protocol FlutterStreamHandler <NSObject>
- (FlutterError* _Nullable)onListenWithArguments:(id _Nullable)arguments
                                       eventSink:(FlutterEventSink)events;
- (FlutterError* _Nullable)onCancelWithArguments:(id _Nullable)arguments;
@end

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterChannels.h#L400C1-L400C42
// Forward declaration (opaque): provided by the host app's Flutter.framework at runtime.
// setStreamHandler is performed on the Swift side (setUpEventChannel closure), so the
// Kotlin side never references this class -> no _OBJC_CLASS_$_FlutterEventChannel symbol.
@class FlutterEventChannel;

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/ios/framework/Headers/FlutterPlugin.h#L189
@protocol FlutterPlugin <NSObject>
@end

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterBinaryMessenger.h#L49C1-L49C44
@protocol FlutterBinaryMessenger <NSObject>
@end

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/ios/framework/Headers/FlutterPlugin.h#L283
@protocol FlutterPluginRegistrar <NSObject>
- (NSObject<FlutterBinaryMessenger>*)messenger;
- (void)addMethodCallDelegate:(NSObject<FlutterPlugin>*)delegate
        channel:(FlutterMethodChannel*)channel;
@end

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterCodecs.h#L220
// Kept as a full @interface (NOT opaque, unlike the classes above): the generated Kotlin
// reads .method / .arguments, so it needs the members. It stays symbol-clean because Kotlin
// only sends messages to instances it RECEIVES (selectors), never references the class
// object — so no _OBJC_CLASS_$_FlutterMethodCall symbol is emitted. Do NOT "make this
// consistent" by turning it into @class; that would break property access.
@interface FlutterMethodCall : NSObject
@property(readonly, nonatomic) NSString* method;
@property(readonly, nonatomic, nullable) id arguments;
@end
