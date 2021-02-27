import 'dart:async';

import 'package:app/model/module.dart';
import 'package:app/model/moduleLayout.dart';
import 'package:app/model/moduleMessage.dart';
import 'package:app/model/pageLayout.dart';
import 'package:app/plugins/couchpotato/module.dart';
import 'package:app/plugins/systemInfo/module.dart';
import 'package:app/widgets/module.dart';
import 'package:app/widgets/moduleOverlay.dart';
import 'package:flutter/material.dart';

class DashboardWidget extends StatefulWidget {
  ModuleLayout moduleLayout;
  StreamController<ModuleMessage> stream;
  bool editMode;
  int selectedId;
  Function selectForEdit, refreshLayout;
  PageLayout pageLayout;
  Key key;

  DashboardWidget(
      {this.key,
      this.stream,
      this.moduleLayout,
      this.editMode,
      this.selectedId,
      this.selectForEdit,
      this.refreshLayout,
      this.pageLayout});


  setEditMode(bool editMode){
    this.editMode = editMode;
  }

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
    List<Widget> stackChildren = <Widget>[
      getModule(widget.moduleLayout.module)
    ];

    if (widget.editMode) {
      stackChildren.add(ModuleOverlay(
          pageLayout: widget.pageLayout,
          layout: widget.moduleLayout,
          refreshLayout: widget.refreshLayout,
          selected: widget.selectedId == widget.moduleLayout.module.id));
    }

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
            child: Stack(
              children: stackChildren,
            )));
  }
}
