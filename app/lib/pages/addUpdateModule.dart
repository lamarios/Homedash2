import 'dart:math';

import 'package:app/model/plugin.dart';
import 'package:app/widgets/pluginSettings.dart';
import 'package:flutter/material.dart';

class AddUpdateModule extends StatefulWidget {
  int pageId;
  Plugin plugin;

  AddUpdateModule({this.pageId, this.plugin});

  @override
  State<AddUpdateModule> createState() => AddUpdateModuleState();
}

class AddUpdateModuleState extends State<AddUpdateModule> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: Text(widget.plugin.displayName + " settings"),
        ),
        body: Padding(
            padding: EdgeInsets.symmetric(
                vertical: 0,
                horizontal:
                    max((MediaQuery.of(context).size.width - 600) / 2, 0)),
            child: Column(children: [
              PluginSettings(plugin: widget.plugin, pageId: widget.pageId)
            ])));
  }
}
