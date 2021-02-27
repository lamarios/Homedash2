import 'package:json_annotation/json_annotation.dart';

part 'layout.g.dart';

@JsonSerializable()

class Layout {
  int id;
  String name;
  int maxGridWidth;

  Layout({this.id, this.name, this.maxGridWidth});

  factory Layout.fromJson(Map<String, dynamic> json)  => _$LayoutFromJson(json);
  Map<String, dynamic> toJson() => _$LayoutToJson(this);
}
