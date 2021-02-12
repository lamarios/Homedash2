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
  ModuleWidget getModule(Module plugin, ModuleMessage data) {
    switch (plugin.pluginClass) {
      case "com.ftpix.homedash.plugins.couchpotato.CouchPotatoPlugin":
        return CouchPotato(data);
      case "com.ftpix.homedash.plugins.SystemInfoPlugin":
        return SystemInfo(data);
    }
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
            child: StreamBuilder<ModuleMessage>(
              stream: widget.stream.stream,
              builder: (context, snapshot) {
                if (snapshot.hasData) {
                  return getModule(widget.moduleLayout.module, snapshot.data);
                } else {
                  return Center(
                      heightFactor: 1.0, child: CircularProgressIndicator());
                }
              },
            )));
  }
}
