import 'package:json_annotation/json_annotation.dart';

part 'ServerConfig.g.dart';

@JsonSerializable()
class ServerConfig {
  @JsonKey(defaultValue: false, name: "auth")
  bool useAuth;

  ServerConfig({this.useAuth});

  factory ServerConfig.fromJson(Map<String, dynamic> json) =>
      _$ServerConfigFromJson(json);

  Map<String, dynamic> toJson() => _$ServerConfigToJson(this);
}
