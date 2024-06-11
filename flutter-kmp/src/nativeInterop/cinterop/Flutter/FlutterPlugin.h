// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/ios/framework/Headers/FlutterPlugin.h

#import <Foundation/Foundation.h>

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterBinaryMessenger.h#L49C1-L49C44
@protocol FlutterBinaryMessenger <NSObject>
@end

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterChannels.h#L194
typedef void (^FlutterResult)(id _Nullable result);

// https://github.com/flutter/engine/blob/3.22.2/shell/platform/darwin/common/framework/Headers/FlutterChannels.h#L220
@interface FlutterMethodChannel : NSObject
- (instancetype)initWithName:(NSString*)name
                messenger:(NSObject<FlutterBinaryMessenger>*)messenger;
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