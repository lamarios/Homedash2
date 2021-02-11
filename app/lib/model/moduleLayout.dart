import 'package:app/model/module.dart';

class ModuleLayout {
  Module module;
  int id;
  int x;
  int y;
  String size;

  ModuleLayout({this.module, this.id, this.x, this.y, this.size});

  factory ModuleLayout.fromJson(Map<String, dynamic> json) {
    return ModuleLayout(
        id: json['id'] as int,
        x: json['x'] as int,
        y: json['y'] as int,
        size: json['size'] as String,
        module: Module.fromJson(json['module'] as Map<String, dynamic>));
  }
}
