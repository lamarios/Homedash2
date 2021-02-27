import 'package:app/model/layout.dart';
import 'package:app/model/moduleLayout.dart';
import 'package:json_annotation/json_annotation.dart';

part 'pageLayout.g.dart';

@JsonSerializable(explicitToJson: true)
class PageLayout {
  Layout layout;
  List<ModuleLayout> modules;

  PageLayout({this.layout, this.modules});

  factory PageLayout.fromJson(Map<String, dynamic> json) =>
      _$PageLayoutFromJson(json);

  Map<String, dynamic> toJson() => _$PageLayoutToJson(this);

/*
  factory PageLayout.fromJson(Map<String, dynamic> json) {
    return PageLayout(
        layout: Layout.fromJson(json['layout'] as Map<String, dynamic>),
        modules: (json['modules'] as List)
            .map((e) => ModuleLayout.fromJson(e))
            .toList());
  }
*/
}
