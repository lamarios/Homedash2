// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'moduleLayout.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ModuleLayout _$ModuleLayoutFromJson(Map<String, dynamic> json) {
  return ModuleLayout(
    module: json['module'] == null
        ? null
        : Module.fromJson(json['module'] as Map<String, dynamic>),
    id: json['id'] as int,
    x: json['x'] as int,
    y: json['y'] as int,
    size: json['size'] as String,
  );
}

Map<String, dynamic> _$ModuleLayoutToJson(ModuleLayout instance) =>
    <String, dynamic>{
      'module': instance.module?.toJson(),
      'id': instance.id,
      'x': instance.x,
      'y': instance.y,
      'size': instance.size,
    };
