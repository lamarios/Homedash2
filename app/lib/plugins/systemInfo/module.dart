import 'package:app/model/moduleMessage.dart';
import 'package:app/widgets/module.dart';
import 'package:flutter/material.dart';

class SystemInfo extends ModuleWidget {
  @override
  State<StatefulWidget> createState() => SystemInfoState();

  SystemInfo(ModuleMessage lastMessage) : super(lastMessage: lastMessage);
}

class SystemInfoState extends State<SystemInfo> {
  @override
  Widget build(BuildContext context) {
    return Container(
        child: Text(widget.lastMessage == null
            ? 'System info'
            : widget.lastMessage.command));
  }
}
