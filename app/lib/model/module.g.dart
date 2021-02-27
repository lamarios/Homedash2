// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'module.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Module _$ModuleFromJson(Map<String, dynamic> json) {
  return Module(
    id: json['id'] as int,
    pluginClass: json['pluginClass'] as String,
    location: json['location'] as String,
    onKiosk: json['onKiosk'] as bool,
  );
}

Map<String, dynamic> _$ModuleToJson(Module instance) => <String, dynamic>{
      'id': instance.id,
      'pluginClass': instance.pluginClass,
      'location': instance.location,
      'onKiosk': instance.onKiosk,
    };
