
// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/ios/framework/Headers/FlutterPlugin.h

#import <Foundation/Foundation.h>

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterBinaryMessenger.h#L49C1-L49C44
@protocol FlutterBinaryMessenger <NSObject>
@end

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterChannels.h#L194
typedef void (^FlutterResult)(id _Nullable result);

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterCodecs.h#L391
@protocol FlutterMethodCodec
+ (instancetype)sharedInstance;
@end

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterCodecs.h#L18C1-L18C30
@protocol FlutterMessageCodec
+ (instancetype)sharedInstance;
@end

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterChannels.h#L220
@interface FlutterMethodChannel : NSObject
- (instancetype)initWithName:(NSString*)name
             binaryMessenger:(NSObject<FlutterBinaryMessenger>*)messenger
                       codec:(NSObject<FlutterMessageCodec>*)codec;
@end

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterChannels.h#L350
typedef void (^FlutterEventSink)(id _Nullable event);

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterCodecs.h#L246C1-L246C35
@interface FlutterError : NSObject
+ (instancetype)errorWithCode:(NSString*)code
                      message:(NSString* _Nullable)message
                      details:(id _Nullable)details;
@end

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterChannels.h#L356
@protocol FlutterStreamHandler <NSObject>
- (FlutterError* _Nullable)onListenWithArguments:(id _Nullable)arguments
                                       eventSink:(FlutterEventSink)events;
- (FlutterError* _Nullable)onCancelWithArguments:(id _Nullable)arguments;
@end

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterChannels.h#L400C1-L400C42
@interface FlutterEventChannel : NSObject
- (instancetype)initWithName:(NSString*)name
             binaryMessenger:(NSObject<FlutterBinaryMessenger>*)messenger
                       codec:(NSObject<FlutterMethodCodec>*)codec;
- (void)setStreamHandler:(NSObject<FlutterStreamHandler>* _Nullable)handler;
@end

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/ios/framework/Headers/FlutterPlugin.h#L189
@protocol FlutterPlugin <NSObject>
@end

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/ios/framework/Headers/FlutterPlugin.h#L283
@protocol FlutterPluginRegistrar <NSObject>
- (NSObject<FlutterBinaryMessenger>*)messenger;
- (void)addMethodCallDelegate:(NSObject<FlutterPlugin>*)delegate
        channel:(FlutterMethodChannel*)channel;
@end

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterCodecs.h#L220
@interface FlutterMethodCall : NSObject
@property(readonly, nonatomic) NSString* method;
@property(readonly, nonatomic, nullable) id arguments;
@end

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterCodecs.h#L469
@interface FlutterStandardMethodCodec : NSObject <FlutterMethodCodec>
@end
