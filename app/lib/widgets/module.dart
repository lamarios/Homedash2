import 'dart:async';

import 'package:app/model/moduleMessage.dart';
import 'package:flutter/cupertino.dart';

abstract class ModuleWidget extends StatefulWidget{
  ModuleMessage lastMessage;

  ModuleWidget({this.lastMessage});
}

