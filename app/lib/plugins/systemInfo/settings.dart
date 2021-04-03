import 'package:app/widgets/pluginSettings.dart';
import 'package:flutter/material.dart';

class SystemInfoSettings extends PluginSettingsParent {
  bool notify = false;

  @override
  State<StatefulWidget> createState() => SystemInfoSettingsState();

  SystemInfoSettings(Map<String, String> settings){
    restoreSettings(settings);
  }

  void setNotified(value) {
    this.notify = value;
  }

  @override
  Map<String, String> save() {
    var settings = Map<String, String>();
    settings.putIfAbsent("notifications", () => notify ? "1" : "0");

    return settings;
  }

  @override
  void restoreSettings(Map<String, String> settings) {
    // TODO: implement restoreSettings
  }
}

class SystemInfoSettingsState extends State<SystemInfoSettings> {
  setNotified(value) {
    setState(() {
      widget.notify = value;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Row(children: [
      Checkbox(value: widget.notify, onChanged: setNotified),
      Expanded(child: Text('Notify me when the RAM or CPU load is high'))
    ]);
  }
}
