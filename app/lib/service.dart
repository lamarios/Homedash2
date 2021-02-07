import 'dart:convert';

import 'package:app/model/ServerConfig.dart';
import 'package:app/model/page.dart';
import 'package:http/http.dart' as http;
import 'package:jwt_decoder/jwt_decoder.dart';
import 'package:shared_preferences/shared_preferences.dart';

class Service {
  String url;
  Map<String, String> headers = Map();

  Service(this.url);

  Future<bool> setToken(String token) async {

    final prefs = await SharedPreferences.getInstance();
    prefs.setString("token", token);

    token = "Bearer " + token;

    headers.update("Authorization", (value) => token, ifAbsent: () => token);

    return true;
  }

  /// Gets the server config, to know if authentication is required for example
  Future<ServerConfig> getConfig() async {
    final response = await http.get(url + "/config");
    if (response.statusCode == 200) {
      return ServerConfig.fromJson(toJson(response));
    } else {
      throw Exception("Couldn't get server config");
    }
  }

  static isTokenExpired(String token) {
    token = token.replaceAll("Bearer ", "");
    return JwtDecoder.isExpired(token);
  }

  Future<List<PluginPage>> getPages() async {
    final response = await http.get(url + "/pages", headers: headers);
    if (response.statusCode == 200) {
      Iterable i = toJson(response);
      return List<PluginPage>.from(i.map((e) => PluginPage.fromJson(e)));
    } else {
      throw Exception("Couldn't get pages ${response.body}");
    }
  }

  /// Logs in to the server
  Future<bool> login(String username, String password) async {
    Map<String, String> creds = Map();
    creds.putIfAbsent("username", () => username);
    creds.putIfAbsent("password", () => password);

    final response = await http.post(url + "/login", body: creds);

    if (response.statusCode == 401) {
      throw Exception("Invalid username/password combination");
    } else if (response.statusCode == 200) {
      final tokenSet = await setToken(response.body);
      return tokenSet;
    } else {
      throw Exception("Error while connecting to server");
    }
  }

  toJson(http.Response response) {
    return jsonDecode(response.body);
  }
}
