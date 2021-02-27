// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'layout.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Layout _$LayoutFromJson(Map<String, dynamic> json) {
  return Layout(
    id: json['id'] as int,
    name: json['name'] as String,
    maxGridWidth: json['maxGridWidth'] as int,
  );
}

Map<String, dynamic> _$LayoutToJson(Layout instance) => <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'maxGridWidth': instance.maxGridWidth,
    };
