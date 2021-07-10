import 'dart:async';

import 'package:app/model/moduleMessage.dart';
import 'package:flutter/cupertino.dart';

abstract class ModuleWidget extends StatefulWidget {
  StreamController<ModuleMessage> stream;

  ModuleWidget({Key key, this.stream});
}

abstract class ModuleWidgetState<T extends ModuleWidget> extends State<T> {}
