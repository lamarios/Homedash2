import 'package:app/model/moduleMessage.dart';
import 'package:app/widgets/module.dart';
import 'package:flutter/material.dart';

class CouchPotato extends ModuleWidget {
  @override
  State<StatefulWidget> createState() => CouchPotatoState();

  CouchPotato(ModuleMessage lastMessage) : super(lastMessage: lastMessage);
}

class CouchPotatoState extends State<CouchPotato> {
  @override
  Widget build(BuildContext context) {
    return Container(child: Text('couchpotato'));
  }
}
