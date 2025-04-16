import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_kmp_example/MySecondTestModule.dart';
import 'package:flutter_kmp_example/MyTestModule.dart';
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
  late StreamSubscription<int> _subscription;
  late StreamSubscription<int> _subscription2;
  late StreamSubscription<int?> _intStateSubscription;
  late StreamSubscription<int?> _intStateAddSubscription;
  late StreamSubscription<bool?> _boolStateSubscription;
  late StreamSubscription<bool> _boolEventsSubscription;
  late StreamSubscription<MyDataClass?> _dataClassStateSubscription;
  late StreamSubscription<MyDataClass?> _parameterizedDataClassFlowSubscription;
  late StreamSubscription<MyDataClass> _dataClassEventsSubscription;
  late StreamSubscription<MyDataClass> _secondModuleDataClassEventsSubscription;

  @override
  void initState() {
    super.initState();
    init();
  }

  Future<void> init() async {
    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    final myTestModule = MyTestModule();

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

    await myTestModule.unitMethod();
    print(await myTestModule.simpleMethod());
    print(await myTestModule.stringMethod("test"));
    print(await myTestModule.nullableStringMethod(null));
    print(await myTestModule.intMethod(1));
    print(await myTestModule.doubleMethod(1.0));
    print(await myTestModule.boolMethod(false));
    await myTestModule.parameterizedMethod("dwa", 123, false, 213.3);

    await myTestModule.suspendUnitMethod();
    print(await myTestModule.suspendStringMethod());
    final result = await myTestModule.dataClassMethod(data);
    print(result.stringProp);

    print(await myTestModule.nullableDataClassMethod(null));
    print(await myTestModule.nullableEnumClassMethod(null));
    print(await myTestModule.nullableObjectMethod(null));
    print(await myTestModule.nullableSealedClassMethod(null));

    print(await myTestModule.stringListMethod(["hello", "world"]));
    print(await myTestModule.nestedListMethod([
      ["hello", "world"]
    ]));
    print(await myTestModule.dataClassListMethod([data]));
    print(await myTestModule.nestedDataClassListMethod([
      [data]
    ]));
    print(await myTestModule.mapMethod({'a': 123, 'b': 456}));
    print(await myTestModule.mixedMethod({
      'a': [
        {'a': data}
      ]
    }));

    print(await myTestModule.objectMethod(MyDataObject()));

    final sealedData =
        MySealedDataOption1("dwa", MySealedDataOption1Nested("dwa"));
    print(await myTestModule.sealedMethod(sealedData));

    print(await myTestModule.enumMethod(MyEnum.CASE_1));
    print(await myTestModule.enumListMethod([MyEnum.CASE_1, MyEnum.CASE_2]));
    print(await myTestModule
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

    print(await myTestModule.classWithSealedPropMethod(dataWithSealedData));

    print(await myTestModule.instantMethod(DateTime.now().toUtc()));
    print(await myTestModule.localDateTimeMethod(DateTime.now()));
    print(await myTestModule.localDateMethod(DateTime.now()));
    print(await myTestModule.localTimeMethod(TimeOfDay.now()));
    print(await myTestModule.durationMethod(const Duration(seconds: 123)));
    print(await myTestModule.durationMethod(const Duration(days: 1000)));

    print(await myTestModule.dateClassMethod(MyDateClass(
        DateTime.now(),
        TimeOfDay.now(),
        DateTime.now(),
        const Duration(seconds: 123),
        DateTime.now().toUtc())));

    print(await myTestModule.dateClassMethod(MyDateClass(
        DateTime.now(),
        TimeOfDay.now(),
        DateTime.now(),
        const Duration(days: 1000),
        DateTime.now().toUtc())));

    final broadcaster = myTestModule.intEvents;

    // this starts a collect on the counter flow that is shared across all listeners
    _subscription = broadcaster.listen((item) {
      print("int event subscription 1: $item");
    });

    await Future.delayed(const Duration(milliseconds: 2000));

    // because this subscription comes later it will not have the first event
    _subscription2 = broadcaster.listen((item) {
      print("int event subscription 2: $item");
    });

    _intStateSubscription = myTestModule.intState((item) {
      print("int state value: $item");
    });

    _intStateAddSubscription = myTestModule.intStateAdd(20, (item) {
      print("int state add value: $item");
    });

    _boolStateSubscription = myTestModule.boolState((item) {
      print("boolean state flow value: $item");
    });

    _boolEventsSubscription = myTestModule.boolEvents.listen((item) {
      print("boolean event: $item");
    });

    _dataClassStateSubscription = myTestModule.dataClassState((item) {
      print("data class state value: $item");
    });

    _parameterizedDataClassFlowSubscription =
        myTestModule.parameterizedDataClassState(data, (item) {
      print("parameterized data class state value: $item");
    });

    _dataClassEventsSubscription = myTestModule.dataClassEvents.listen((item) {
      print("data class events: $item");
    });

    final mySecondTestModule = MySecondTestModule();

    print(
        "myTestModule.methodWithSameNameAsInOtherModule: ${await myTestModule.methodWithSameNameAsInOtherModule("abc")}");
    print(
        "mySecondTestModule.methodWithSameNameAsInOtherModule: ${await mySecondTestModule.methodWithSameNameAsInOtherModule(123)}");

    print(
        "mySecondTestModule.testMethod: ${await mySecondTestModule.testMethod()}");

    _secondModuleDataClassEventsSubscription =
        mySecondTestModule.dataClassEvents.listen((item) {
      print("mySecondTestModule.dataClassEvents: $item");
    });
  }

  @override
  void dispose() {
    _subscription.cancel();
    _subscription2.cancel();
    _intStateSubscription.cancel();
    _intStateAddSubscription.cancel();
    _boolStateSubscription.cancel();
    _boolEventsSubscription.cancel();
    _dataClassStateSubscription.cancel();
    _parameterizedDataClassFlowSubscription.cancel();
    _dataClassEventsSubscription.cancel();
    _secondModuleDataClassEventsSubscription.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Flutter KMP example app'),
        ),
        body: const Center(
          child: Text('Hello!'),
        ),
      ),
    );
  }
}
