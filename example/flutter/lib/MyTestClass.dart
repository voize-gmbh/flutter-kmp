// Generated by flutter-kmp. Do not modify.

import 'dart:async';
import 'dart:convert';
import 'package:iso_duration/iso_duration.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'models.dart';
               
class MyTestClass {
  final methodChannelToNative = const MethodChannel("MyTestClass");
  
  final Stream<int> counterEvents = const EventChannel('MyTestClass_counterEvents')
    .receiveBroadcastStream()
    .map((event) => jsonDecode(event) as int);
final Stream<MyDataClass> myDataClassEvents = const EventChannel('MyTestClass_myDataClassEvents')
    .receiveBroadcastStream()
    .map((event) => MyDataClass.fromJson(jsonDecode(event) as Map<String, dynamic>));
          StreamSubscription<int?> counter(Function(int?) onData) {
    final streamController = StreamController<int?>();
    

    Future<int?> next(int? previous) async {
    return await methodChannelToNative.invokeMethod<int>(
            'counter',
            [previous]
        );
    }
    
    void startEmittingValues() async {
        int? currentValue;
        while (!streamController.isClosed) {
            try {
                currentValue = await next(currentValue);
                if (!streamController.isClosed) {
                    if (currentValue == null) {
                        streamController.add(null);
                    } else {
                        streamController.add(currentValue);
                    }
                }
            } catch (e) {
                if (!streamController.isClosed) {
                    streamController.addError(e);
                }
            }
        }
    }
    
    streamController.onListen = startEmittingValues;
    
    return streamController.stream.listen(onData);
}
        StreamSubscription<MyDataClass?> myDataClassFlow(Function(MyDataClass?) onData) {
    final streamController = StreamController<MyDataClass?>();
    

    Future<String?> next(String? previous) async {
    return await methodChannelToNative.invokeMethod<String>(
            'myDataClassFlow',
            [previous]
        );
    }
    
    void startEmittingValues() async {
        String? currentValue;
        while (!streamController.isClosed) {
            try {
                currentValue = await next(currentValue);
                if (!streamController.isClosed) {
                    if (currentValue == null) {
                        streamController.add(null);
                    } else {
                        streamController.add(MyDataClass.fromJson(jsonDecode(currentValue) as Map<String, dynamic>));
                    }
                }
            } catch (e) {
                if (!streamController.isClosed) {
                    streamController.addError(e);
                }
            }
        }
    }
    
    streamController.onListen = startEmittingValues;
    
    return streamController.stream.listen(onData);
}
        StreamSubscription<MyDataClass?> myParameterizedDataClassFlow(MyDataClass data, Function(MyDataClass?) onData) {
    final streamController = StreamController<MyDataClass?>();
    final dataSerialized = jsonEncode(data.toJson());

    Future<String?> next(String? previous) async {
    return await methodChannelToNative.invokeMethod<String>(
            'myParameterizedDataClassFlow',
            [previous, dataSerialized]
        );
    }
    
    void startEmittingValues() async {
        String? currentValue;
        while (!streamController.isClosed) {
            try {
                currentValue = await next(currentValue);
                if (!streamController.isClosed) {
                    if (currentValue == null) {
                        streamController.add(null);
                    } else {
                        streamController.add(MyDataClass.fromJson(jsonDecode(currentValue) as Map<String, dynamic>));
                    }
                }
            } catch (e) {
                if (!streamController.isClosed) {
                    streamController.addError(e);
                }
            }
        }
    }
    
    streamController.onListen = startEmittingValues;
    
    return streamController.stream.listen(onData);
}
        StreamSubscription<int?> counterPlus(int num, Function(int?) onData) {
    final streamController = StreamController<int?>();
    

    Future<int?> next(int? previous) async {
    return await methodChannelToNative.invokeMethod<int>(
            'counterPlus',
            [previous, num]
        );
    }
    
    void startEmittingValues() async {
        int? currentValue;
        while (!streamController.isClosed) {
            try {
                currentValue = await next(currentValue);
                if (!streamController.isClosed) {
                    if (currentValue == null) {
                        streamController.add(null);
                    } else {
                        streamController.add(currentValue);
                    }
                }
            } catch (e) {
                if (!streamController.isClosed) {
                    streamController.addError(e);
                }
            }
        }
    }
    
    streamController.onListen = startEmittingValues;
    
    return streamController.stream.listen(onData);
}
  Future<void> unitMethod() async {
    
    await methodChannelToNative.invokeMethod<void>('unitMethod', []);
}
Future<String> stringMethod() async {
    
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'stringMethod',
        [],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method stringMethod failed');
    }

    final result = invokeResult;

    return result;
}
Future<String?> nullableStringMethod() async {
    
    final invokeResult = await methodChannelToNative.invokeMethod<String?>(
        'nullableStringMethod',
        [],
    );
    final result = invokeResult;
    return result;
}
Future<String?> nullableMethod() async {
    
    final invokeResult = await methodChannelToNative.invokeMethod<String?>(
        'nullableMethod',
        [],
    );
    final result = invokeResult;
    return result;
}
Future<int> intMethod() async {
    
    final invokeResult = await methodChannelToNative.invokeMethod<int>(
        'intMethod',
        [],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method intMethod failed');
    }

    final result = invokeResult;

    return result;
}
Future<double> doubleMethod() async {
    
    final invokeResult = await methodChannelToNative.invokeMethod<double>(
        'doubleMethod',
        [],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method doubleMethod failed');
    }

    final result = invokeResult;

    return result;
}
Future<bool> boolMethod() async {
    
    final invokeResult = await methodChannelToNative.invokeMethod<bool>(
        'boolMethod',
        [],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method boolMethod failed');
    }

    final result = invokeResult;

    return result;
}
Future<void> parameterizedMethod(String a, int b, bool c, double d) async {
    
    await methodChannelToNative.invokeMethod<void>('parameterizedMethod', [a, b, c, d]);
}
Future<DateTime> localDateTimeMethod(DateTime localDateTime) async {
    if (localDateTime.isUtc) throw ArgumentError('localDateTime must not be in UTC');
final localDateTimeSerialized = localDateTime.toIso8601String();
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'localDateTimeMethod',
        [localDateTimeSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method localDateTimeMethod failed');
    }

    final result = DateTime.parse(invokeResult);

    return result;
}
Future<TimeOfDay> localTimeMethod(TimeOfDay localTime) async {
    final localTimeSerialized = "${localTime.hour.toString().padLeft(2, '0')}:${localTime.minute.toString().padLeft(2, '0')}";
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'localTimeMethod',
        [localTimeSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method localTimeMethod failed');
    }

    final result = TimeOfDay.fromDateTime(DateTime.parse("1998-01-01T$invokeResult:00.000"));

    return result;
}
Future<DateTime> localDateMethod(DateTime localDate) async {
    if (localDate.isUtc) throw ArgumentError('localDate must not be in UTC');
final localDateSerialized = localDate.toIso8601String().split('T').first;
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'localDateMethod',
        [localDateSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method localDateMethod failed');
    }

    final result = DateTime.parse(invokeResult);

    return result;
}
Future<Duration> durationMethod(Duration duration) async {
    final durationSerialized = duration.toIso8601String();
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'durationMethod',
        [durationSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method durationMethod failed');
    }

    final result = parseIso8601Duration(invokeResult);

    return result;
}
Future<DateTime> instantMethod(DateTime instant) async {
    if (!instant.isUtc) throw ArgumentError('instant must be in UTC');
final instantSerialized = instant.toIso8601String();
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'instantMethod',
        [instantSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method instantMethod failed');
    }

    final result = DateTime.parse(invokeResult);

    return result;
}
Future<List<String>> stringListMethod(List<String> list) async {
    final listSerialized = jsonEncode(list.map((e) => e).toList());
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'stringListMethod',
        [listSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method stringListMethod failed');
    }

    final result = (jsonDecode(invokeResult) as List<dynamic>).map((element) {
return element as String;
}).toList();

    return result;
}
Future<List<List<String>>> nestedListMethod(List<List<String>> list) async {
    final listSerialized = jsonEncode(list.map((e) => e.map((e) => e).toList()).toList());
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'nestedListMethod',
        [listSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method nestedListMethod failed');
    }

    final result = (jsonDecode(invokeResult) as List<dynamic>).map((element) {
return (element as List<dynamic>).map((element) {
return element as String;
}).toList();
}).toList();

    return result;
}
Future<List<MyDataClass>> dataClassListMethod(List<MyDataClass> list) async {
    final listSerialized = jsonEncode(list.map((e) => e).toList());
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'dataClassListMethod',
        [listSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method dataClassListMethod failed');
    }

    final result = (jsonDecode(invokeResult) as List<dynamic>).map((element) {
return MyDataClass.fromJson(element);
}).toList();

    return result;
}
Future<List<List<MyDataClass>>> nestedDataClassListMethod(List<List<MyDataClass>> list) async {
    final listSerialized = jsonEncode(list.map((e) => e.map((e) => e).toList()).toList());
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'nestedDataClassListMethod',
        [listSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method nestedDataClassListMethod failed');
    }

    final result = (jsonDecode(invokeResult) as List<dynamic>).map((element) {
return (element as List<dynamic>).map((element) {
return MyDataClass.fromJson(element);
}).toList();
}).toList();

    return result;
}
Future<Map<String, int>> mapMethod(Map<String, int> map) async {
    final mapSerialized = jsonEncode(map.map((k, v) => MapEntry(k, v)));
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'mapMethod',
        [mapSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method mapMethod failed');
    }

    final result = (jsonDecode(invokeResult) as Map<String, dynamic>).map((key, value) {
return MapEntry(key, value as int);
});

    return result;
}
Future<MyDataObject> objectMethod(MyDataObject obj) async {
    final objSerialized = jsonEncode(obj.toJson());
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'objectMethod',
        [objSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method objectMethod failed');
    }

    final result = MyDataObject.fromJson(jsonDecode(invokeResult) as Map<String, dynamic>);

    return result;
}
Future<MySealedData> sealedMethod(MySealedData obj) async {
    final objSerialized = jsonEncode(MySealedData.toJson(obj));
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'sealedMethod',
        [objSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method sealedMethod failed');
    }

    final result = MySealedData.fromJson(jsonDecode(invokeResult));

    return result;
}
Future<MyDateClass> dateClassMethod(MyDateClass obj) async {
    final objSerialized = jsonEncode(obj.toJson());
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'dateClassMethod',
        [objSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method dateClassMethod failed');
    }

    final result = MyDateClass.fromJson(jsonDecode(invokeResult) as Map<String, dynamic>);

    return result;
}
Future<MySealedDataWithProps> sealedWithPropsMethod(MySealedDataWithProps obj) async {
    final objSerialized = jsonEncode(MySealedDataWithProps.toJson(obj));
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'sealedWithPropsMethod',
        [objSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method sealedWithPropsMethod failed');
    }

    final result = MySealedDataWithProps.fromJson(jsonDecode(invokeResult));

    return result;
}
Future<MyDataClassWithSealed> classWithSealedPropMethod(MyDataClassWithSealed obj) async {
    final objSerialized = jsonEncode(obj.toJson());
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'classWithSealedPropMethod',
        [objSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method classWithSealedPropMethod failed');
    }

    final result = MyDataClassWithSealed.fromJson(jsonDecode(invokeResult) as Map<String, dynamic>);

    return result;
}
Future<MyEnum> enumMethod(MyEnum entry) async {
    final entrySerialized = jsonEncode(entry.name);;
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'enumMethod',
        [entrySerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method enumMethod failed');
    }

    final result = MyEnum.values.byName(jsonDecode(invokeResult));

    return result;
}
Future<List<MyEnum>> enumListMethod(List<MyEnum> entries) async {
    final entriesSerialized = jsonEncode(entries.map((e) => e.name).toList());
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'enumListMethod',
        [entriesSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method enumListMethod failed');
    }

    final result = (jsonDecode(invokeResult) as List<dynamic>).map((element) {
return MyEnum.values.byName(element);
}).toList();

    return result;
}
Future<Map<String, MyEnum>> enumMapMethod(Map<String, MyEnum> entries) async {
    final entriesSerialized = jsonEncode(entries.map((k, v) => MapEntry(k, v.name)));
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'enumMapMethod',
        [entriesSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method enumMapMethod failed');
    }

    final result = (jsonDecode(invokeResult) as Map<String, dynamic>).map((key, value) {
return MapEntry(key, MyEnum.values.byName(value));
});

    return result;
}
Future<Map<String, List<Map<String, MyDataClass>>>> mixedMethod(Map<String, List<Map<String, MyDataClass>>> map) async {
    final mapSerialized = jsonEncode(map.map((k, v) => MapEntry(k, v.map((e) => e.map((k, v) => MapEntry(k, v))).toList())));
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'mixedMethod',
        [mapSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method mixedMethod failed');
    }

    final result = (jsonDecode(invokeResult) as Map<String, dynamic>).map((key, value) {
return MapEntry(key, (value as List<dynamic>).map((element) {
return (element as Map<String, dynamic>).map((key, value) {
return MapEntry(key, MyDataClass.fromJson(value));
});
}).toList());
});

    return result;
}
Future<MyDataClass> dataClassMethod(MyDataClass data) async {
    final dataSerialized = jsonEncode(data.toJson());
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'dataClassMethod',
        [dataSerialized],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method dataClassMethod failed');
    }

    final result = MyDataClass.fromJson(jsonDecode(invokeResult) as Map<String, dynamic>);

    return result;
}
Future<void> suspendUnitMethod() async {
    
    await methodChannelToNative.invokeMethod<void>('suspendUnitMethod', []);
}
Future<String> suspendStringMethod() async {
    
    final invokeResult = await methodChannelToNative.invokeMethod<String>(
        'suspendStringMethod',
        [],
    );

    if (invokeResult == null) {
        throw PlatformException(code: '1', message: 'Method suspendStringMethod failed');
    }

    final result = invokeResult;

    return result;
}
}