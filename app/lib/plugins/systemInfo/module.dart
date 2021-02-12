import 'package:app/model/moduleMessage.dart';
import 'package:app/plugins/systemInfo/models.dart';
import 'package:app/widgets/module.dart';
import 'package:filesize/filesize.dart';
import 'package:flutter/material.dart';

class SystemInfo extends ModuleWidget {
  @override
  State<StatefulWidget> createState() => SystemInfoState();

  SystemInfo(ModuleMessage lastMessage) : super(lastMessage: lastMessage);
}

class SystemInfoState extends State<SystemInfo> {
  @override
  Widget build(BuildContext context) {
    Refresh refresh = Refresh.fromJson(widget.lastMessage.message);
    return Container(
        child: Column(children: [
      Text(refresh.cpuInfo[0].cpuUsage.toString()),
      Text(filesize(refresh.ramInfo[0].availableRam))
    ]));
  }
}
