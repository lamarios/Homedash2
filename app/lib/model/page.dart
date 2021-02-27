import 'package:json_annotation/json_annotation.dart';

part 'page.g.dart';

@JsonSerializable()
class PluginPage {
  int id;
  String name;

  PluginPage({this.id, this.name});

  factory PluginPage.fromJson(Map<String, dynamic> json) => _$PluginPageFromJson(json);
  Map<String, dynamic> toJson() => _$PluginPageToJson(this);
}
