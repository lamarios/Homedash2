import 'package:app/globals.dart' as globals;
import 'package:app/model/plugin.dart';
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

  Widget getSettings() {
    PluginSettingsParent parent = null;
    switch (widget.plugin.className) {
      case "com.ftpix.homedash.plugins.SystemInfoPlugin":
        parent = SystemInfoSettings();
        break;
    }

    currentPluginSettings = parent;
    return parent;
  }

  void save() async {
    var settings = currentPluginSettings.save();
    print("${settings}");
    var errors = await globals.service.saveModule(widget.pageId, widget.plugin.className, settings);
    print('${errors}');
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        getSettings(),
        TextButton(onPressed: save, child: Text('Save'))
      ],
    );
  }
}

abstract class PluginSettingsParent extends StatefulWidget {
  Map<String, String> save();

  void restoreSettings(Map<String, String> settings);
}
