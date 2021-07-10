import 'package:json_annotation/json_annotation.dart';

part 'module.g.dart';

@JsonSerializable()
class Module {
  int id;
  String pluginClass;
  String location;
  bool onKiosk;
int order;
  Module({this.id, this.pluginClass, this.location, this.onKiosk, this.order});

  factory Module.fromJson(Map<String, dynamic> json) => _$ModuleFromJson(json);
  Map<String, dynamic> toJson() => _$ModuleToJson(this);
}
