import 'package:app/widgets/pluginSettings.dart';
import 'package:flutter/material.dart';

class CouchpotatoSettings extends PluginSettingsParent {
  String type = 'COUCHPOTATO';
  String url;
  String apiKey;

  CouchpotatoSettings(Map<String, String> settings) {
    restoreSettings(settings);
  }

  @override
  State<StatefulWidget> createState() => CouchpotatoSettingsState();

  void setType(String value) {
    this.type = value;
  }

  void setUrl(String value) {
    this.url = value;
  }

  void setApiKey(String value) {
    this.apiKey = value;
  }

  @override
  Map<String, String> save() {
    var settings = Map<String, String>();
    settings.putIfAbsent("type", () => type);
    settings.putIfAbsent("url", () => url);
    settings.putIfAbsent("apiKey", () => apiKey);

    return settings;
  }

  @override
  void restoreSettings(Map<String, String> settings) {
    // TODO: implement restoreSettings
    if (settings.containsKey("type")) {
      this.type = settings["type"];
    }

    if (settings.containsKey("url")) {
      this.url = settings["url"];
    }

    if (settings.containsKey("apiKey")) {
      this.apiKey = settings["apiKey"];
    }
  }
}

class CouchpotatoSettingsState extends State<CouchpotatoSettings> {
  @override
  Widget build(BuildContext context) {
    print('rebuiilding settings');
    return Column(children: [
      DropdownButtonFormField<String>(
        decoration: const InputDecoration(labelText: 'Type', enabled: true),
        onChanged: widget.setType,
        value: widget.type,
        items: [
          DropdownMenuItem(value: 'COUCHPOTATO', child: Text('Couchpotato')),
          DropdownMenuItem(value: 'RADARR', child: Text('Radarr'))
        ],
      ),
      TextFormField(
        decoration: const InputDecoration(
          hintText: 'http://couchpotato.mydomain.com',
          labelText: 'URL',
        ),
        enableSuggestions: false,
        autocorrect: false,
        onChanged: widget.setUrl,
        initialValue: widget.url,
      ),
      TextFormField(
        decoration: const InputDecoration(
          hintText: '',
          labelText: 'Api Key',
        ),
        enableSuggestions: false,
        autocorrect: false,
        onChanged: widget.setApiKey,
        initialValue: widget.apiKey,
      )
    ]);
  }
}
