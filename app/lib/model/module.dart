import 'package:json_annotation/json_annotation.dart';

part 'module.g.dart';

@JsonSerializable()
class Module {
  int id;
  String pluginClass;
  String location;
  bool onKiosk;

  Module({this.id, this.pluginClass, this.location, this.onKiosk});

  factory Module.fromJson(Map<String, dynamic> json) => _$ModuleFromJson(json);
  Map<String, dynamic> toJson() => _$ModuleToJson(this);
}
