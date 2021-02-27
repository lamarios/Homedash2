
import 'package:json_annotation/json_annotation.dart';

part 'plugin.g.dart';

@JsonSerializable()
class Plugin {
  String className;
  String displayName;
  String description;
  bool settings;

  Plugin({this.className, this.displayName, this.description, this.settings});

  factory Plugin.fromJson(Map<String, dynamic> json) => _$PluginFromJson(json);
  Map<String, dynamic> toJson() => _$PluginToJson(this);
}
