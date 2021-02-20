import 'dart:async';

import 'package:app/model/module.dart';
import 'package:app/model/moduleLayout.dart';
import 'package:app/model/moduleMessage.dart';
import 'package:app/plugins/couchpotato/module.dart';
import 'package:app/plugins/systemInfo/module.dart';
import 'package:app/widgets/module.dart';
import 'package:flutter/material.dart';

class DashboardWidget extends StatefulWidget {
  ModuleLayout moduleLayout;
  StreamController<ModuleMessage> stream;

  DashboardWidget({Key key, this.stream, this.moduleLayout});

  @override
  _DashboardWidgetState createState() => _DashboardWidgetState();
}

class _DashboardWidgetState extends State<DashboardWidget> {
  ModuleWidget getModule(Module plugin) {
    var key = Key(plugin.id.toString());

    var split = widget.moduleLayout.size.split('x');
    int width = int.parse(split[0]);
    int height = int.parse(split[1]);

    ModuleWidget moduleWidget;
    switch (plugin.pluginClass) {
      case "com.ftpix.homedash.plugins.couchpotato.CouchPotatoPlugin":
        moduleWidget = CouchPotato(
          key: key,
          stream: widget.stream,
          width: width,
          height: height,
        );
        break;
      case "com.ftpix.homedash.plugins.SystemInfoPlugin":
        moduleWidget = SystemInfo(
            key: key, stream: widget.stream, width: width, height: height);
        break;
    }

    return moduleWidget;
  }

  @override
  void dispose() {
    super.dispose();
    print('disposing');
    widget.stream.close();
  }

  @override
  Widget build(BuildContext context) {
    // TODO: implement build
    return Container(
        decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.all(Radius.circular(5)),
            boxShadow: [
              BoxShadow(
                color: Colors.grey.withOpacity(0.5),
                spreadRadius: 2,
                blurRadius: 5,
                offset: Offset(2, 2), // changes position of shadow
              ),
            ]),
        child: ClipRRect(
            borderRadius: BorderRadius.circular(5),
            child: getModule(widget.moduleLayout.module)));
  }
}
