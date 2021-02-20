import 'dart:async';

import 'package:app/model/moduleMessage.dart';
import 'package:flutter/cupertino.dart';

abstract class ModuleWidget extends StatefulWidget {
  StreamController<ModuleMessage> stream;
  int width;
  int height;

  ModuleWidget({Key key, this.stream, this.width, this.height});
}

abstract class ModuleWidgetState<T extends ModuleWidget> extends State<T> {}
