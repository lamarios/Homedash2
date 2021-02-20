import 'dart:async';

import 'package:app/globals.dart' as g;
import 'package:app/model/moduleMessage.dart';
import 'package:app/widgets/module.dart';
import 'package:flutter/material.dart';

import 'models.dart';

class CouchPotato extends ModuleWidget {
  @override
  State<StatefulWidget> createState() => CouchPotatoState();

  CouchPotato(
      {Key key, StreamController<ModuleMessage> stream, int width, int height})
      : super(key: key, stream: stream, width: width, height: height);
}

class CouchPotatoState extends ModuleWidgetState<CouchPotato> {
  Refresh refresh;

  @override
  void initState() {
    super.initState();
    widget.stream.stream.listen(onMessage);
  }

  void onMessage(ModuleMessage message) {
    if (message.command == 'refresh') {
      Refresh refresh = Refresh.fromJson(message.message);
      setState(() {
        this.refresh = refresh;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (refresh != null) {
      final url = g.service.url + refresh.poster;
      return Container(
          decoration: BoxDecoration(
            image: DecorationImage(image: NetworkImage(url), fit: BoxFit.cover),
          ),
          child: Container(
              color: Color.fromRGBO(0, 0, 0, 0.5),
              child: Stack(
                children: [],
              )));
    } else {
      return Center(heightFactor: 1.0, child: CircularProgressIndicator());
    }
  }
}
