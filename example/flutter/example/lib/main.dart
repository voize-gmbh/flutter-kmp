import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_kmp_example/MyTestClass.dart';
import 'package:flutter_kmp_example/flutter_kmp_example.dart';
import 'package:flutter_kmp_example/models.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  final _flutterKmpExamplePlugin = FlutterKmpExample();
  late StreamSubscription<int> _subscription;
  late StreamSubscription<int> _subscription2;
  late StreamSubscription<int?> _subscription3;
  late StreamSubscription<int?> _subscription4;
  late StreamSubscription<MyDataClass?> _subscription5;
  late StreamSubscription<MyDataClass?> _subscription6;
  late StreamSubscription<MyDataClass> _subscription7;

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      platformVersion = await _flutterKmpExamplePlugin.getPlatformVersion() ??
          'Unknown platform version';
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    final myTestClass = MyTestClass();

    final data = MyDataClass(
      "dwa",
      null,
      123,
      null,
      false,
      null,
      123.3,
      null,
      123.3,
      null,
      MyDataClassNestedDataClass(
        "dwa",
        123,
        false,
        123.3,
      ),
      null,
      ["a", "b"],
      null,
      ["a", null],
      {"a": 1, "b": 2},
      null,
      {"a": 1, "b": null},
      null,
    );

    () async {
      await myTestClass.unitMethod();
      print(await myTestClass.stringMethod());
      await myTestClass.suspendUnitMethod();
      print(await myTestClass.suspendStringMethod());
      print(await myTestClass.nullableMethod());
      await myTestClass.parameterizedMethod("dwa", 123, false, 213.3);

      final result = await myTestClass.dataClassMethod(data);

      print(result.stringProp);

      print(await myTestClass.stringListMethod(["hello", "world"]));
      print(await myTestClass.nestedListMethod([
        ["hello", "world"]
      ]));
      print(await myTestClass.dataClassListMethod([data]));
      print(await myTestClass.nestedDataClassListMethod([
        [data]
      ]));
      print(await myTestClass.mapMethod({'a': 123, 'b': 456}));
      print(await myTestClass.mixedMethod({
        'a': [
          {'a': data}
        ]
      }));

      print(await myTestClass.objectMethod(MyDataObject()));

      final sealedData =
          MySealedDataOption1("dwa", MySealedDataOption1Nested("dwa"));
      print(await myTestClass.sealedMethod(sealedData));

      print(await myTestClass.enumMethod(MyEnum.CASE_1));
      print(await myTestClass.enumListMethod([MyEnum.CASE_1, MyEnum.CASE_2]));
      print(await MyTestClass()
          .enumMapMethod({'a': MyEnum.CASE_1, 'b': MyEnum.CASE_2}));

      final dataWithSealedData = MyDataClassWithSealed(
        sealedData,
        null,
        [sealedData],
        null,
        [
          [sealedData]
        ],
        {'a': sealedData},
        null,
        {
          'a': {'a': sealedData}
        },
        {
          'a': [
            {'a': sealedData}
          ]
        },
      );

      print(await myTestClass.classWithSealedPropMethod(dataWithSealedData));

      print(await myTestClass.instantMethod(DateTime.now().toUtc()));
      print(await myTestClass.localDateTimeMethod(DateTime.now()));
      print(await myTestClass.localDateMethod(DateTime.now()));
      print(await myTestClass.localTimeMethod(TimeOfDay.now()));
      print(await myTestClass.durationMethod(const Duration(seconds: 123)));

      print(await myTestClass.dateClassMethod(MyDateClass(
          DateTime.now(),
          TimeOfDay.now(),
          DateTime.now(),
          const Duration(seconds: 123),
          DateTime.now().toUtc())));
    }();

    final broadcaster = myTestClass.counterEvents;

    // this starts a collect on the counter flow that is shared across all listeners
    _subscription = broadcaster.listen((item) {
      print("flow event subscription 1: $item");
    });

    await Future.delayed(const Duration(milliseconds: 2000));

    // because this subscription comes later it will not have the first event
    _subscription2 = broadcaster.listen((item) {
      print("flow event subscription 2: $item");
    });

    _subscription3 = myTestClass.counter((item) {
      print("state flow value: $item");
    });

    _subscription4 = myTestClass.counterPlus(20, (item) {
      print("state flow value: $item");
    });

    _subscription5 = myTestClass.myDataClassFlow((item) {
      print("data class state flow value: $item");
    });

    _subscription6 = myTestClass.myParameterizedDataClassFlow(data, (item) {
      print("parameterized data class state flow value: $item");
    });

    _subscription7 = myTestClass.myDataClassEvents.listen((item) {
      print("data class events: $item");
    });

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  void dispose() {
    _subscription.cancel();
    _subscription2.cancel();
    _subscription3.cancel();
    _subscription4.cancel();
    _subscription5.cancel();
    _subscription6.cancel();
    _subscription7.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Text('Running on: $_platformVersion\n'),
        ),
      ),
    );
  }
}
