// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'moduleMessage.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ModuleMessage _$ModuleMessageFromJson(Map<String, dynamic> json) {
  return ModuleMessage(
    id: json['id'] as int,
    command: json['command'] as String,
    message: json['message'],
  );
}

Map<String, dynamic> _$ModuleMessageToJson(ModuleMessage instance) =>
    <String, dynamic>{
      'command': instance.command,
      'message': instance.message,
      'id': instance.id,
    };
