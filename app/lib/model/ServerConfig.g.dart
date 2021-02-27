// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'ServerConfig.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ServerConfig _$ServerConfigFromJson(Map<String, dynamic> json) {
  return ServerConfig(
    useAuth: json['auth'] as bool ?? false,
  );
}

Map<String, dynamic> _$ServerConfigToJson(ServerConfig instance) =>
    <String, dynamic>{
      'auth': instance.useAuth,
    };
