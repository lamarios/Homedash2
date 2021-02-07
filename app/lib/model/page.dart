class PluginPage {
  int id;
  String name;

  PluginPage({this.id, this.name});

  factory PluginPage.fromJson(Map<String, dynamic> json) {
    return PluginPage(id: json['id'] as int, name: json['name'] as String);
  }
}
