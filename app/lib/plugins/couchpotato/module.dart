import 'package:app/globals.dart' as g;
import 'package:app/model/moduleMessage.dart';
import 'package:app/widgets/module.dart';
import 'package:flutter/material.dart';

import 'models.dart';

class CouchPotato extends ModuleWidget {
  @override
  State<StatefulWidget> createState() => CouchPotatoState();

  CouchPotato(ModuleMessage lastMessage) : super(lastMessage: lastMessage);
}

class CouchPotatoState extends State<CouchPotato> {
  @override
  Widget build(BuildContext context) {
    if (widget.lastMessage != null && widget.lastMessage.message != null) {
      Refresh refresh = Refresh.fromJson(widget.lastMessage.message);

      final url = g.service.url + refresh.poster;
      return Container(
          decoration: BoxDecoration(
            image: DecorationImage(image: NetworkImage(url), fit: BoxFit.cover),
          ),
          child: Container(
              color: Color.fromRGBO(0, 0, 0, 0.5),
              child: Stack(
                children: [Text('yo')],
              )));
    } else {
      return Container(child: Text('waiting'));
    }
  }
}
