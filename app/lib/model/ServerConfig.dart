class ServerConfig{
  bool useAuth;

  ServerConfig({this.useAuth});

  factory ServerConfig.fromJson(Map<String, dynamic> json){
    return ServerConfig(useAuth: json['auth'] as bool);
  }
}