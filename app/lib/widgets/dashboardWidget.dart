import 'package:app/model/module.dart';
import 'package:app/model/moduleLayout.dart';
import 'package:app/model/moduleMessage.dart';
import 'package:app/plugins/couchpotato/module.dart';
import 'package:app/plugins/systemInfo/module.dart';
import 'package:app/widgets/module.dart';
import 'package:flutter/material.dart';

class DashboardWidget extends StatefulWidget {
  ModuleLayout moduleLayout;
  ModuleMessage lastMessage;

  DashboardWidget({Key key, this.lastMessage, this.moduleLayout});

  @override
  _DashboardWidgetState createState() => _DashboardWidgetState();
}

class _DashboardWidgetState extends State<DashboardWidget> {
  ModuleWidget getModule(Module plugin) {
    switch (plugin.pluginClass) {
      case "com.ftpix.homedash.plugins.couchpotato.CouchPotatoPlugin":
        return CouchPotato(widget.lastMessage);
      case "com.ftpix.homedash.plugins.SystemInfoPlugin":
        return SystemInfo(widget.lastMessage);
    }
  }

  @override
  Widget build(BuildContext context) {
    // TODO: implement build
    return Container(
        color: Colors.green, child: getModule(widget.moduleLayout.module));
  }
}
