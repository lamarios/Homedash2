class Module {
  int id;
  String pluginClass;
  String location;
  bool onKiosk;

  Module({this.id, this.pluginClass, this.location, this.onKiosk});

  factory Module.fromJson(Map<String, dynamic> json) {
    return Module(
        id: json['id'] as int,
        pluginClass: json['pluginClass'] as String,
        location: json['location'] as String,
        onKiosk: json['onKiosk'] as bool);
  }
}
