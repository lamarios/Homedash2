import 'package:app/globals.dart' as globals;
import 'package:app/model/plugin.dart';
import 'package:app/plugins/couchpotato/settings.dart';
import 'package:app/plugins/systemInfo/settings.dart';
import 'package:flutter/material.dart';

class PluginSettings extends StatefulWidget {
  Plugin plugin;
  int pageId;

  PluginSettings({this.plugin, this.pageId});

  @override
  State<StatefulWidget> createState() => PluginSettingsState();
}

class PluginSettingsState extends State<PluginSettings> {
  PluginSettingsParent currentPluginSettings;
  Map<String, dynamic> errors = Map();

  Map<String, String> lastSettings = Map();

  Widget getSettings() {
    PluginSettingsParent parent = null;
    switch (widget.plugin.className) {
      case "com.ftpix.homedash.plugins.SystemInfoPlugin":
        parent = SystemInfoSettings(lastSettings);
        break;
      case "com.ftpix.homedash.plugins.couchpotato.CouchPotatoPlugin":
        parent = CouchpotatoSettings(lastSettings);
        break;
    }

    currentPluginSettings = parent;
    return parent;
  }

  void save() async {
    var settings = currentPluginSettings.save();
    var errors = await globals.service
        .saveModule(widget.pageId, widget.plugin.className, settings);

    if (errors.isEmpty) {
      int count = 0;
      Navigator.popUntil(context, (route) {
        return count++ == 3;
      });
    } else {
      setState(() {
        this.lastSettings = settings;
        this.errors = errors;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    var errorsWidgets = <Widget>[];

    errors.forEach((key, value) {
      errorsWidgets.add(Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            key + ": ",
            style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white),
          ),
          Expanded(
              child: Text(
            value,
            style: TextStyle(color: Colors.white),
          ))
        ],
      ));
    });

    return Column(
      children: [
        errors.isNotEmpty
            ? Padding(
                padding: EdgeInsets.fromLTRB(30, 20, 30, 10),
                child: Card(
                  color: Colors.red,
                  child: Padding(
                      padding: EdgeInsets.all(20),
                      child: Column(
                        children: errorsWidgets,
                      )),
                ),
              )
            : SizedBox.shrink(),
        Padding(
            padding: EdgeInsets.fromLTRB(30, 20, 30, 10),
            child: Card(
              child: Padding(
                  padding: EdgeInsets.all(20),
                  child: Column(children: [
                    getSettings(),
                  ])),
            )),
        ElevatedButton(onPressed: save, child: Text('Save'))
      ],
    );
  }
}

abstract class PluginSettingsParent extends StatefulWidget {
  Map<String, String> save();

  void restoreSettings(Map<String, String> settings);
}
