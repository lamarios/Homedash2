import 'package:json_annotation/json_annotation.dart';

part 'moduleMessage.g.dart';

@JsonSerializable()
class ModuleMessage {
  String command;
  dynamic message;
  int id;

  ModuleMessage({this.id, this.command, this.message});

  factory ModuleMessage.fromJson(Map<String, dynamic> json) =>
      _$ModuleMessageFromJson(json);

  Map<String, dynamic> toJson() => _$ModuleMessageToJson(this);
}
