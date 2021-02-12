
class Refresh {
  String name;
  String poster;

  Refresh({this.name, this.poster});

  factory Refresh.fromJson(Map<String, dynamic> json){
   return Refresh(name: json['name'] as String, poster: json['poster'] as String);
  }
}
