import 'package:app/model/layout.dart';
import 'package:app/model/moduleLayout.dart';

class PageLayout {
  Layout layout;
  List<ModuleLayout> modules;

  PageLayout({this.layout, this.modules});

  factory PageLayout.fromJson(Map<String, dynamic> json) {
    return PageLayout(
        layout: Layout.fromJson(json['layout'] as Map<String, dynamic>),
        modules: (json['modules'] as List)
            .map((e) => ModuleLayout.fromJson(e))
            .toList());
  }
}
