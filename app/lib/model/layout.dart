class Layout {
  int id;
  String name;
  int maxGridWidth;

  Layout({this.id, this.name, this.maxGridWidth});

  factory Layout.fromJson(Map<String, dynamic> json) {
    return Layout(
        id: json['id'] as int,
        name: json['name'] as String,
        maxGridWidth: json['maxGridWidth']);
  }
}
