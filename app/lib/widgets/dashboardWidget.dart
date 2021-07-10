import 'dart:async';

import 'package:app/model/module.dart';
import 'package:app/model/moduleMessage.dart';
import 'package:app/plugins/couchpotato/module.dart';
import 'package:app/plugins/systemInfo/module.dart';
import 'package:app/widgets/module.dart';
import 'package:app/widgets/moduleOverlay.dart';
import 'package:flutter/material.dart';

class DashboardWidget extends StatefulWidget {
  Module module;
  final StreamController<ModuleMessage> stream =
      StreamController<ModuleMessage>();
  bool editMode;
  int selectedId;
  Function selectForEdit, refreshLayout;
  Key key;
  int pageId;
  bool isLast;

  DashboardWidget(
      {this.key,
      this.pageId,
      this.module,
      this.editMode,
      this.selectedId,
      this.selectForEdit,
      this.refreshLayout,
      this.isLast});

  setEditMode(bool editMode) {
    this.editMode = editMode;
  }

  setLastMessage(ModuleMessage message) {
    if (!stream.isClosed) stream.add(message);
  }

  int getModuleId() {
    return this.module.id;
  }

  @override
  _DashboardWidgetState createState() => _DashboardWidgetState();
}

class _DashboardWidgetState extends State<DashboardWidget> {
  ModuleWidget getModule(Module plugin) {
    var key = Key(plugin.id.toString());

    ModuleWidget moduleWidget;
    switch (plugin.pluginClass) {
      case "com.ftpix.homedash.plugins.couchpotato.CouchPotatoPlugin":
        moduleWidget = CouchPotato(
          key: key,
          stream: widget.stream,
        );
        break;
      case "com.ftpix.homedash.plugins.SystemInfoPlugin":
        moduleWidget = SystemInfo(key: key, stream: widget.stream);
        break;
    }

    return moduleWidget;
  }

  @override
  void dispose() {
    super.dispose();
    print('disposing');
    if (!widget.stream.isClosed) {
      widget.stream.close();
    }
  }

  @override
  Widget build(BuildContext context) {
    List<Widget> stackChildren = <Widget>[getModule(widget.module)];

    if (widget.editMode) {
      stackChildren.add(ModuleOverlay(
          module: widget.module,
          refreshLayout: widget.refreshLayout,
          pageId: widget.pageId,
          isLast: widget.isLast,
          selected: widget.selectedId == widget.module.id));
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
