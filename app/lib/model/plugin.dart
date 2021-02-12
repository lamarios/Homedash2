class Plugin {
  String className;
  String displayName;
  String description;
  bool settings;

  Plugin({this.className, this.displayName, this.description, this.settings});

  factory Plugin.fromJson(Map<String, dynamic> json) {
    return Plugin(
        className: json['className'] as String,
        displayName: json['displayName'] as String,
        description: json['description'] as String,
        settings: json['settings'] as bool);
  }
}
