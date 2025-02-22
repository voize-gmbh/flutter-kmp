import 'package:json_annotation/json_annotation.dart';
import 'package:flutter/material.dart';

import 'package:iso_duration/iso_duration.dart';

part 'models.g.dart';

@JsonSerializable(explicitToJson: true)
class MyDataObject {


MyDataObject();



factory MyDataObject.fromJson(Map<String, dynamic> json) => _$MyDataObjectFromJson(json);

Map<String, dynamic> toJson() => _$MyDataObjectToJson(this);
}
sealed class MySealedData {


MySealedData();



factory MySealedData.fromJson(Map<String, dynamic> json) {
    final discriminator = json['type'] as String;

    switch (discriminator) {
    case 'option1':
    return MySealedDataOption1.fromJson(json);
case 'option2':
    return MySealedDataOption2.fromJson(json);
case 'option3':
    return MySealedDataOption3.fromJson(json);
        default:
            throw Exception('Unknown class: $discriminator');
    }
}

static Map<String, dynamic> toJson(MySealedData obj) {
    switch (obj.runtimeType) {
    case MySealedDataOption1:
	final data = (obj as MySealedDataOption1).toJson();
    data['type'] = "option1";
    return data;
case MySealedDataOption2:
	final data = (obj as MySealedDataOption2).toJson();
    data['type'] = "option2";
    return data;
case MySealedDataOption3:
	final data = (obj as MySealedDataOption3).toJson();
    data['type'] = "option3";
    return data;
        default:
            throw Exception('Unknown class: $obj');
    }
}
                            }
@JsonSerializable(explicitToJson: true)
class MyDateClass {
@JsonKey(toJson: _dateToJson, fromJson: _dateFromJson)
final DateTime date;

@JsonKey(toJson: _timeToJson, fromJson: _timeFromJson)
final TimeOfDay time;

final DateTime dateTime;

@JsonKey(toJson: _durationToJson, fromJson: _durationFromJson)
final Duration duration;

final DateTime instant;


MyDateClass(this.date, this.time, this.dateTime, this.duration, this.instant);


        static String _dateToJson(DateTime obj) => obj.toIso8601String().split('T').first;
        static DateTime _dateFromJson(String json) => DateTime.parse(json);
        

        static String _timeToJson(TimeOfDay obj) => "${obj.hour.toString().padLeft(2, '0')}:${obj.minute.toString().padLeft(2, '0')}";
        static TimeOfDay _timeFromJson(String json) => TimeOfDay.fromDateTime(DateTime.parse("1998-01-01T$json:00.000"));
        

        static String _durationToJson(Duration obj) => obj.toIso8601String().replaceFirstMapped(RegExp(r'P([^T]*)(T.*)?'), (m) {
        int totalDays = 0;
        String datePart = m[1]!
          .replaceAllMapped(RegExp(r'(\d+)Y'), (y) { totalDays += int.parse(y[1]!) * 365; return ''; })
          .replaceAllMapped(RegExp(r'(\d+)M'), (m) { totalDays += int.parse(m[1]!) * 30; return ''; })
          .replaceAllMapped(RegExp(r'(\d+)W'), (w) { totalDays += int.parse(w[1]!) * 7; return ''; })
          .replaceAllMapped(RegExp(r'(\d+)D'), (d) { totalDays += int.parse(d[1]!); return ''; });

        return 'P' + (totalDays > 0 ? '${totalDays}D' : '') + (m[2] ?? '');
    });
        static Duration _durationFromJson(String json) => parseIso8601Duration(json);
        

factory MyDateClass.fromJson(Map<String, dynamic> json) => _$MyDateClassFromJson(json);

Map<String, dynamic> toJson() => _$MyDateClassToJson(this);
}
sealed class MySealedDataWithProps {
final String name;


MySealedDataWithProps(this.name);



factory MySealedDataWithProps.fromJson(Map<String, dynamic> json) {
    final discriminator = json['type'] as String;

    switch (discriminator) {
    case 'option1':
    return MySealedDataWithPropsOption1.fromJson(json);
case 'option2':
    return MySealedDataWithPropsOption2.fromJson(json);
        default:
            throw Exception('Unknown class: $discriminator');
    }
}

static Map<String, dynamic> toJson(MySealedDataWithProps obj) {
    switch (obj.runtimeType) {
    case MySealedDataWithPropsOption1:
	final data = (obj as MySealedDataWithPropsOption1).toJson();
    data['type'] = "option1";
    return data;
case MySealedDataWithPropsOption2:
	final data = (obj as MySealedDataWithPropsOption2).toJson();
    data['type'] = "option2";
    return data;
        default:
            throw Exception('Unknown class: $obj');
    }
}
                            }
@JsonSerializable(explicitToJson: true)
class MyDataClassWithSealed {
@JsonKey(toJson: _dataToJson, fromJson: _dataFromJson)
final MySealedData data;

@JsonKey(toJson: _nullableDataToJson, fromJson: _nullableDataFromJson)
final MySealedData? nullableData;

@JsonKey(toJson: _dataListToJson, fromJson: _dataListFromJson)
final List<MySealedData> dataList;

@JsonKey(toJson: _nullableDataListToJson, fromJson: _nullableDataListFromJson)
final List<MySealedData>? nullableDataList;

@JsonKey(toJson: _dataNestedListToJson, fromJson: _dataNestedListFromJson)
final List<List<MySealedData>> dataNestedList;

@JsonKey(toJson: _dataMapToJson, fromJson: _dataMapFromJson)
final Map<String, MySealedData> dataMap;

@JsonKey(toJson: _nullableDataMapToJson, fromJson: _nullableDataMapFromJson)
final Map<String, MySealedData>? nullableDataMap;

@JsonKey(toJson: _dataNestedMapToJson, fromJson: _dataNestedMapFromJson)
final Map<String, Map<String, MySealedData>> dataNestedMap;

@JsonKey(toJson: _dataComplexToJson, fromJson: _dataComplexFromJson)
final Map<String, List<Map<String, MySealedData>>> dataComplex;


MyDataClassWithSealed(this.data, this.nullableData, this.dataList, this.nullableDataList, this.dataNestedList, this.dataMap, this.nullableDataMap, this.dataNestedMap, this.dataComplex);


        static Map<String, dynamic> _dataToJson(MySealedData obj) => MySealedData.toJson(obj);
        static MySealedData _dataFromJson(Map<String, dynamic> json) => MySealedData.fromJson(json);
        

        static Map<String, dynamic>? _nullableDataToJson(MySealedData? obj) => obj == null ? null : MySealedData.toJson(obj);
        static MySealedData? _nullableDataFromJson(Map<String, dynamic>? json) => json == null ? null : MySealedData.fromJson(json);
        

        static List<dynamic> _dataListToJson(List<MySealedData> obj) => obj.map((e) => MySealedData.toJson(e)).toList();
        static List<MySealedData> _dataListFromJson(List<dynamic> json) => (json as List<dynamic>).map((e) => MySealedData.fromJson(e)).toList();
        

        static List<dynamic>? _nullableDataListToJson(List<MySealedData>? obj) => obj == null ? null : obj.map((e) => MySealedData.toJson(e)).toList();
        static List<MySealedData>? _nullableDataListFromJson(List<dynamic>? json) => json == null ? null : (json as List<dynamic>).map((e) => MySealedData.fromJson(e)).toList();
        

        static List<dynamic> _dataNestedListToJson(List<List<MySealedData>> obj) => obj.map((e) => e.map((e) => MySealedData.toJson(e)).toList()).toList();
        static List<List<MySealedData>> _dataNestedListFromJson(List<dynamic> json) => (json as List<dynamic>).map((e) => (e as List<dynamic>).map((e) => MySealedData.fromJson(e)).toList()).toList();
        

        static Map<String, dynamic> _dataMapToJson(Map<String, MySealedData> obj) => (obj as Map<String, dynamic>).map((k, e) => MapEntry(k, MySealedData.toJson(e)));
        static Map<String, MySealedData> _dataMapFromJson(Map<String, dynamic> json) => (json as Map<String, dynamic>).map((k, e) => MapEntry(k, MySealedData.fromJson(e)));
        

        static Map<String, dynamic>? _nullableDataMapToJson(Map<String, MySealedData>? obj) => obj == null ? null : (obj as Map<String, dynamic>).map((k, e) => MapEntry(k, MySealedData.toJson(e)));
        static Map<String, MySealedData>? _nullableDataMapFromJson(Map<String, dynamic>? json) => json == null ? null : (json as Map<String, dynamic>).map((k, e) => MapEntry(k, MySealedData.fromJson(e)));
        

        static Map<String, dynamic> _dataNestedMapToJson(Map<String, Map<String, MySealedData>> obj) => (obj as Map<String, dynamic>).map((k, e) => MapEntry(k, (e as Map<String, dynamic>).map((k, e) => MapEntry(k, MySealedData.toJson(e)))));
        static Map<String, Map<String, MySealedData>> _dataNestedMapFromJson(Map<String, dynamic> json) => (json as Map<String, dynamic>).map((k, e) => MapEntry(k, (e as Map<String, dynamic>).map((k, e) => MapEntry(k, MySealedData.fromJson(e)))));
        

        static Map<String, dynamic> _dataComplexToJson(Map<String, List<Map<String, MySealedData>>> obj) => (obj as Map<String, dynamic>).map((k, e) => MapEntry(k, e.map((e) => (e as Map<String, dynamic>).map((k, e) => MapEntry(k, MySealedData.toJson(e)))).toList()));
        static Map<String, List<Map<String, MySealedData>>> _dataComplexFromJson(Map<String, dynamic> json) => (json as Map<String, dynamic>).map((k, e) => MapEntry(k, (e as List<dynamic>).map((e) => (e as Map<String, dynamic>).map((k, e) => MapEntry(k, MySealedData.fromJson(e)))).toList()));
        

factory MyDataClassWithSealed.fromJson(Map<String, dynamic> json) => _$MyDataClassWithSealedFromJson(json);

Map<String, dynamic> toJson() => _$MyDataClassWithSealedToJson(this);
}
enum MyEnum {
CASE_1, CASE_2, CASE_3
}
@JsonSerializable(explicitToJson: true)
class MyDataClass {
final String stringProp;

final String? nullableStringProp;

final int intProp;

final int? nullableIntProp;

final bool boolProp;

final bool? nullableBoolProp;

final double floatProp;

final double? nullableFloatProp;

final double doubleProp;

final double? nullableDoubleProp;

final MyDataClassNestedDataClass nestedClassProp;

final MyDataClassNestedDataClass? nullableNestedClassProp;

final List<String> listProp;

final List<String>? nullableListProp;

final List<String?> listOfNullableProp;

final Map<String, int> mapProp;

final Map<String, int>? nullableMapProp;

final Map<String, int?> mapOfNullableProp;

final String? nullableWithDefault;


MyDataClass(this.stringProp, this.nullableStringProp, this.intProp, this.nullableIntProp, this.boolProp, this.nullableBoolProp, this.floatProp, this.nullableFloatProp, this.doubleProp, this.nullableDoubleProp, this.nestedClassProp, this.nullableNestedClassProp, this.listProp, this.nullableListProp, this.listOfNullableProp, this.mapProp, this.nullableMapProp, this.mapOfNullableProp, this.nullableWithDefault);



factory MyDataClass.fromJson(Map<String, dynamic> json) => _$MyDataClassFromJson(json);

Map<String, dynamic> toJson() => _$MyDataClassToJson(this);
}
@JsonSerializable(explicitToJson: true)
class MySealedDataOption1 extends MySealedData {
final String name;

final MySealedDataOption1Nested nested;


MySealedDataOption1(this.name, this.nested);



factory MySealedDataOption1.fromJson(Map<String, dynamic> json) => _$MySealedDataOption1FromJson(json);

Map<String, dynamic> toJson() => _$MySealedDataOption1ToJson(this);
}
@JsonSerializable(explicitToJson: true)
class MySealedDataOption2 extends MySealedData {
final int number;

final MyDataClass nonNested;


MySealedDataOption2(this.number, this.nonNested);



factory MySealedDataOption2.fromJson(Map<String, dynamic> json) => _$MySealedDataOption2FromJson(json);

Map<String, dynamic> toJson() => _$MySealedDataOption2ToJson(this);
}
@JsonSerializable(explicitToJson: true)
class MySealedDataOption3 extends MySealedData {


MySealedDataOption3();



factory MySealedDataOption3.fromJson(Map<String, dynamic> json) => _$MySealedDataOption3FromJson(json);

Map<String, dynamic> toJson() => _$MySealedDataOption3ToJson(this);
}
@JsonSerializable(explicitToJson: true)
class MySealedDataWithPropsOption1 extends MySealedDataWithProps {
final bool boolProp;


MySealedDataWithPropsOption1(super.name, this.boolProp);



factory MySealedDataWithPropsOption1.fromJson(Map<String, dynamic> json) => _$MySealedDataWithPropsOption1FromJson(json);

Map<String, dynamic> toJson() => _$MySealedDataWithPropsOption1ToJson(this);
}
@JsonSerializable(explicitToJson: true)
class MySealedDataWithPropsOption2 extends MySealedDataWithProps {
final int number;


MySealedDataWithPropsOption2(super.name, this.number);



factory MySealedDataWithPropsOption2.fromJson(Map<String, dynamic> json) => _$MySealedDataWithPropsOption2FromJson(json);

Map<String, dynamic> toJson() => _$MySealedDataWithPropsOption2ToJson(this);
}
@JsonSerializable(explicitToJson: true)
class MyDataClassNestedDataClass {
final String stringProp;

final int intProp;

final bool boolProp;

final double doubleProp;


MyDataClassNestedDataClass(this.stringProp, this.intProp, this.boolProp, this.doubleProp);



factory MyDataClassNestedDataClass.fromJson(Map<String, dynamic> json) => _$MyDataClassNestedDataClassFromJson(json);

Map<String, dynamic> toJson() => _$MyDataClassNestedDataClassToJson(this);
}
@JsonSerializable(explicitToJson: true)
class MySealedDataOption1Nested {
final String? nullable;


MySealedDataOption1Nested(this.nullable);



factory MySealedDataOption1Nested.fromJson(Map<String, dynamic> json) => _$MySealedDataOption1NestedFromJson(json);

Map<String, dynamic> toJson() => _$MySealedDataOption1NestedToJson(this);
}