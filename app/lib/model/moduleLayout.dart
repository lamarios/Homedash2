import 'package:app/model/module.dart';
import 'package:json_annotation/json_annotation.dart';

part 'moduleLayout.g.dart';

@JsonSerializable(explicitToJson: true)
class ModuleLayout {
  Module module;
  int id;
  int x;
  int y;
  String size;

  ModuleLayout({this.module, this.id, this.x, this.y, this.size});

  factory ModuleLayout.fromJson(Map<String, dynamic> json) => _$ModuleLayoutFromJson(json);

  Map<String, dynamic> toJson() => _$ModuleLayoutToJson(this);
}
