// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'plugin.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Plugin _$PluginFromJson(Map<String, dynamic> json) {
  return Plugin(
    className: json['className'] as String,
    displayName: json['displayName'] as String,
    description: json['description'] as String,
    settings: json['settings'] as bool,
  );
}

Map<String, dynamic> _$PluginToJson(Plugin instance) => <String, dynamic>{
      'className': instance.className,
      'displayName': instance.displayName,
      'description': instance.description,
      'settings': instance.settings,
    };
