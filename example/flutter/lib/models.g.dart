// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'models.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

MyDataObject _$MyDataObjectFromJson(Map<String, dynamic> json) =>
    MyDataObject();

Map<String, dynamic> _$MyDataObjectToJson(MyDataObject instance) =>
    <String, dynamic>{};

MyDateClass _$MyDateClassFromJson(Map<String, dynamic> json) => MyDateClass(
      MyDateClass._dateFromJson(json['date'] as String),
      MyDateClass._timeFromJson(json['time'] as String),
      DateTime.parse(json['dateTime'] as String),
      MyDateClass._durationFromJson(json['duration'] as String),
      DateTime.parse(json['instant'] as String),
    );

Map<String, dynamic> _$MyDateClassToJson(MyDateClass instance) =>
    <String, dynamic>{
      'date': MyDateClass._dateToJson(instance.date),
      'time': MyDateClass._timeToJson(instance.time),
      'dateTime': instance.dateTime.toIso8601String(),
      'duration': MyDateClass._durationToJson(instance.duration),
      'instant': instance.instant.toIso8601String(),
    };

MyDataClassWithSealed _$MyDataClassWithSealedFromJson(
        Map<String, dynamic> json) =>
    MyDataClassWithSealed(
      MyDataClassWithSealed._dataFromJson(json['data'] as Map<String, dynamic>),
      MyDataClassWithSealed._nullableDataFromJson(
          json['nullableData'] as Map<String, dynamic>?),
      MyDataClassWithSealed._dataListFromJson(json['dataList'] as List),
      MyDataClassWithSealed._nullableDataListFromJson(
          json['nullableDataList'] as List?),
      MyDataClassWithSealed._dataNestedListFromJson(
          json['dataNestedList'] as List),
      MyDataClassWithSealed._dataMapFromJson(
          json['dataMap'] as Map<String, dynamic>),
      MyDataClassWithSealed._nullableDataMapFromJson(
          json['nullableDataMap'] as Map<String, dynamic>?),
      MyDataClassWithSealed._dataNestedMapFromJson(
          json['dataNestedMap'] as Map<String, dynamic>),
      MyDataClassWithSealed._dataComplexFromJson(
          json['dataComplex'] as Map<String, dynamic>),
    );

Map<String, dynamic> _$MyDataClassWithSealedToJson(
        MyDataClassWithSealed instance) =>
    <String, dynamic>{
      'data': MyDataClassWithSealed._dataToJson(instance.data),
      'nullableData':
          MyDataClassWithSealed._nullableDataToJson(instance.nullableData),
      'dataList': MyDataClassWithSealed._dataListToJson(instance.dataList),
      'nullableDataList': MyDataClassWithSealed._nullableDataListToJson(
          instance.nullableDataList),
      'dataNestedList':
          MyDataClassWithSealed._dataNestedListToJson(instance.dataNestedList),
      'dataMap': MyDataClassWithSealed._dataMapToJson(instance.dataMap),
      'nullableDataMap': MyDataClassWithSealed._nullableDataMapToJson(
          instance.nullableDataMap),
      'dataNestedMap':
          MyDataClassWithSealed._dataNestedMapToJson(instance.dataNestedMap),
      'dataComplex':
          MyDataClassWithSealed._dataComplexToJson(instance.dataComplex),
    };

MyDataClass _$MyDataClassFromJson(Map<String, dynamic> json) => MyDataClass(
      json['stringProp'] as String,
      json['nullableStringProp'] as String?,
      (json['intProp'] as num).toInt(),
      (json['nullableIntProp'] as num?)?.toInt(),
      json['boolProp'] as bool,
      json['nullableBoolProp'] as bool?,
      (json['floatProp'] as num).toDouble(),
      (json['nullableFloatProp'] as num?)?.toDouble(),
      (json['doubleProp'] as num).toDouble(),
      (json['nullableDoubleProp'] as num?)?.toDouble(),
      MyDataClassNestedDataClass.fromJson(
          json['nestedClassProp'] as Map<String, dynamic>),
      json['nullableNestedClassProp'] == null
          ? null
          : MyDataClassNestedDataClass.fromJson(
              json['nullableNestedClassProp'] as Map<String, dynamic>),
      (json['listProp'] as List<dynamic>).map((e) => e as String).toList(),
      (json['nullableListProp'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList(),
      (json['listOfNullableProp'] as List<dynamic>)
          .map((e) => e as String?)
          .toList(),
      Map<String, int>.from(json['mapProp'] as Map),
      (json['nullableMapProp'] as Map<String, dynamic>?)?.map(
        (k, e) => MapEntry(k, (e as num).toInt()),
      ),
      Map<String, int?>.from(json['mapOfNullableProp'] as Map),
      json['nullableWithDefault'] as String?,
    );

Map<String, dynamic> _$MyDataClassToJson(MyDataClass instance) =>
    <String, dynamic>{
      'stringProp': instance.stringProp,
      'nullableStringProp': instance.nullableStringProp,
      'intProp': instance.intProp,
      'nullableIntProp': instance.nullableIntProp,
      'boolProp': instance.boolProp,
      'nullableBoolProp': instance.nullableBoolProp,
      'floatProp': instance.floatProp,
      'nullableFloatProp': instance.nullableFloatProp,
      'doubleProp': instance.doubleProp,
      'nullableDoubleProp': instance.nullableDoubleProp,
      'nestedClassProp': instance.nestedClassProp.toJson(),
      'nullableNestedClassProp': instance.nullableNestedClassProp?.toJson(),
      'listProp': instance.listProp,
      'nullableListProp': instance.nullableListProp,
      'listOfNullableProp': instance.listOfNullableProp,
      'mapProp': instance.mapProp,
      'nullableMapProp': instance.nullableMapProp,
      'mapOfNullableProp': instance.mapOfNullableProp,
      'nullableWithDefault': instance.nullableWithDefault,
    };

MySealedDataOption1 _$MySealedDataOption1FromJson(Map<String, dynamic> json) =>
    MySealedDataOption1(
      json['name'] as String,
      MySealedDataOption1Nested.fromJson(
          json['nested'] as Map<String, dynamic>),
    );

Map<String, dynamic> _$MySealedDataOption1ToJson(
        MySealedDataOption1 instance) =>
    <String, dynamic>{
      'name': instance.name,
      'nested': instance.nested.toJson(),
    };

MySealedDataOption2 _$MySealedDataOption2FromJson(Map<String, dynamic> json) =>
    MySealedDataOption2(
      (json['number'] as num).toInt(),
      MyDataClass.fromJson(json['nonNested'] as Map<String, dynamic>),
    );

Map<String, dynamic> _$MySealedDataOption2ToJson(
        MySealedDataOption2 instance) =>
    <String, dynamic>{
      'number': instance.number,
      'nonNested': instance.nonNested.toJson(),
    };

MySealedDataOption3 _$MySealedDataOption3FromJson(Map<String, dynamic> json) =>
    MySealedDataOption3();

Map<String, dynamic> _$MySealedDataOption3ToJson(
        MySealedDataOption3 instance) =>
    <String, dynamic>{};

MySealedDataWithPropsOption1 _$MySealedDataWithPropsOption1FromJson(
        Map<String, dynamic> json) =>
    MySealedDataWithPropsOption1(
      json['name'] as String,
      json['boolProp'] as bool,
    );

Map<String, dynamic> _$MySealedDataWithPropsOption1ToJson(
        MySealedDataWithPropsOption1 instance) =>
    <String, dynamic>{
      'name': instance.name,
      'boolProp': instance.boolProp,
    };

MySealedDataWithPropsOption2 _$MySealedDataWithPropsOption2FromJson(
        Map<String, dynamic> json) =>
    MySealedDataWithPropsOption2(
      json['name'] as String,
      (json['number'] as num).toInt(),
    );

Map<String, dynamic> _$MySealedDataWithPropsOption2ToJson(
        MySealedDataWithPropsOption2 instance) =>
    <String, dynamic>{
      'name': instance.name,
      'number': instance.number,
    };

MyDataClassNestedDataClass _$MyDataClassNestedDataClassFromJson(
        Map<String, dynamic> json) =>
    MyDataClassNestedDataClass(
      json['stringProp'] as String,
      (json['intProp'] as num).toInt(),
      json['boolProp'] as bool,
      (json['doubleProp'] as num).toDouble(),
    );

Map<String, dynamic> _$MyDataClassNestedDataClassToJson(
        MyDataClassNestedDataClass instance) =>
    <String, dynamic>{
      'stringProp': instance.stringProp,
      'intProp': instance.intProp,
      'boolProp': instance.boolProp,
      'doubleProp': instance.doubleProp,
    };

MySealedDataOption1Nested _$MySealedDataOption1NestedFromJson(
        Map<String, dynamic> json) =>
    MySealedDataOption1Nested(
      json['nullable'] as String?,
    );

Map<String, dynamic> _$MySealedDataOption1NestedToJson(
        MySealedDataOption1Nested instance) =>
    <String, dynamic>{
      'nullable': instance.nullable,
    };
