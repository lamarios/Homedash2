import 'dart:convert';

import 'package:app/model/ServerConfig.dart';
import 'package:app/model/moduleMessage.dart';
import 'package:app/model/page.dart';
import 'package:app/model/pageLayout.dart';
import 'package:app/model/plugin.dart';
import 'package:app/utils/preferences.dart';
import 'package:http/http.dart' as http;
import 'package:jwt_decoder/jwt_decoder.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

import 'model/moduleLayout.dart';

class Service {
  String url;

  WebSocketChannel websocket;
  Map<String, String> headers = Map();

  Service(this.url);

  /// connect to the websocket
  Stream getWebsocketStream() {
    if (websocket == null) {
      var wsUrl = createWebSocketUrl();
      print(wsUrl);
      websocket = WebSocketChannel.connect(Uri.parse(wsUrl));
    }

    return websocket.stream;
  }

  String createWebSocketUrl() {
    return url.replaceFirst("http", "ws") +
        "/ws?access_token=" +
        headers['Authorization'].replaceAll("Bearer ", "");
  }

  void closeWebSocket() {
    if (websocket.closeCode != null) {
      print('closing socket');
      websocket.sink.close();
    }
  }

  void sendWebsocketMessage(ModuleMessage message) {
    websocket.sink.add(jsonEncode(message.toJson()));
  }

  Future<bool> setToken(String token) async {
    Preferences.set(Preferences.TOKEN, token);

    token = "Bearer " + token;

    headers.update("Authorization", (value) => token, ifAbsent: () => token);

    return true;
  }

  /// Gets the server config, to know if authentication is required for example
  Future<ServerConfig> getConfig() async {
    final response = await http.get(Uri.parse(url + "/config"));
    if (response.statusCode == 200) {
      return ServerConfig.fromJson(toJson(response));
    } else {
      throw Exception("Couldn't get server config");
    }
  }

  /// Gets the current page layout based on the screen width
  Future<PageLayout> getPageLayout(int pageId, int width) async {
    final response = await http.get(
        Uri.parse(url + '/modules-layout/${pageId}/${width}'),
        headers: headers);
    if (response.statusCode == 200) {
      return PageLayout.fromJson(toJson(response));
    } else {
      throw Exception("Couldn't get page layout");
    }
  }

  Future<PageLayout> setPageLayout(
      int pageId, int width, List<ModuleLayout> moduleLayouts) async {
    print(
        'json: ${json.encode(moduleLayouts.map((e) => e.toJson()).toList())}');
    final response = await http.post(
        Uri.parse(url + '/modules-layout/${pageId}/${width}'),
        body: json.encode(moduleLayouts.map((e) => e.toJson()).toList()),
        headers: headers);

    if (response.statusCode == 200) {
      return PageLayout.fromJson(toJson(response));
    } else {
      throw Exception("Couldn't save page layout");
    }
  }

  /// Get the list of available plugins in the system
  Future<List<Plugin>> getAvailablePlugins() async {
    final response =
        await http.get(Uri.parse(url + '/plugins'), headers: headers);
    if (response.statusCode == 200) {
      return (toJson(response) as List).map((e) => Plugin.fromJson(e)).toList();
    } else {
      throw Exception("Couldn't get available plugins");
    }
  }

  static isTokenExpired(String token) {
    token = token.replaceAll("Bearer ", "");
    return JwtDecoder.isExpired(token);
  }

  Future<List<PluginPage>> getPages() async {
    final response =
        await http.get(Uri.parse(url + "/pages"), headers: headers);
    if (response.statusCode == 200) {
      Iterable i = toJson(response);
      return List<PluginPage>.from(i.map((e) => PluginPage.fromJson(e)));
    } else {
      throw Exception("Couldn't get pages ${response.body}");
    }
  }

  Future<String> getModuleNextAvailableSize(
      String currentSize, int moduleId, int maxWidth) async {
    final response =
        await http.post(Uri.parse(url + "/module/getNextAvailableSize"),
            body: {
              "currentSize": currentSize,
              "moduleId": moduleId.toString(),
              "availableWidth": maxWidth.toString(),
            },
            headers: headers);

    if (response.statusCode == 200) {
      return response.body;
    } else {
      throw Exception("Couldn't get module next size ${response.body}");
    }
  }

  Future<void> saveModuleSize(int moduleLayoutId, String size) async {
    final response = await http.get(
        Uri.parse(url +
            "/save-module-size/" +
            moduleLayoutId.toString() +
            "/" +
            size),
        headers: headers);
    if (response.statusCode != 200) {
      throw Exception("Couldn't save module size ${response.body}");
    }
  }

  Future<Map<String, dynamic>> saveModule(
      int page, String clazz, Map<String, String> settings) async {
    settings.putIfAbsent("class", () => clazz);
    print("Pre send: $settings");
    final response = await http.post(
        Uri.parse(url + "/plugins/" + page.toString()),
        headers: headers,
        body: settings);

    if (response.statusCode == 200) {
      var decode = json.decode(response.body);
      print("$decode");
      return decode;
    } else {
      throw Exception("Couldn't save module ${response.body}");
    }
  }

  /// Logs in to the server
  Future<bool> login(String username, String password) async {
    Map<String, String> creds = Map();
    creds.putIfAbsent("username", () => username);
    creds.putIfAbsent("password", () => password);

    final response = await http.post(Uri.parse(url + "/login"), body: creds);

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

  Future<List<PluginPage>> addPage(String name) async {
    Map<String, String> data = new Map();
    data.putIfAbsent("name", () => name);

    final response = await http.post(Uri.parse(url + "/pages/add"),
        headers: headers, body: data);

    if (response.statusCode == 200) {
      Iterable i = toJson(response);
      return List<PluginPage>.from(i.map((e) => PluginPage.fromJson(e)));
    } else {
      throw Exception("Couldn't insert page ${response.body}");
    }
  }

  Future<List<PluginPage>> renamePage(int id, String name) async {
    Map<String, String> data = new Map();
    data.putIfAbsent("name", () => name);

    final response = await http.post(
        Uri.parse(url + "/pages/edit/" + id.toString()),
        headers: headers,
        body: data);

    if (response.statusCode == 200) {
      Iterable i = toJson(response);
      return List<PluginPage>.from(i.map((e) => PluginPage.fromJson(e)));
    } else {
      throw Exception("Couldn't insert page ${response.body}");
    }
  }

  Future<List<PluginPage>> deletePage(int id) async {
    final response = await http.delete(
      Uri.parse(url + "/pages/" + id.toString()),
      headers: headers,
    );

    if (response.statusCode == 200) {
      Iterable i = toJson(response);
      return List<PluginPage>.from(i.map((e) => PluginPage.fromJson(e)));
    } else {
      throw Exception("Couldn't insert page ${response.body}");
    }
  }

  Future<void> moveModule(int moduleLayoutId, bool forward, int pageId) async {
    final response = await http.get(
        Uri.parse(url +
            "/move-module/" +
            moduleLayoutId.toString() +
            "/" +
            forward.toString() +
            "/page/" +
            pageId.toString()),
        headers: headers);
    if (response.statusCode != 200) {
      throw Exception("Couldn't move module ${response.body}");
    }
  }

  Future<void> deleteModule(int id) async {
    final response = await http
        .delete(Uri.parse(url + "/module/" + id.toString()), headers: headers);

    if (response.statusCode != 200) {
      throw Exception("Couldn't delete module ${response.body}");
    }
  }
}
