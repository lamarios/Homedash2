import 'dart:async';
import 'dart:convert';
import 'dart:math';

import 'package:app/model/module.dart';
import 'package:app/model/moduleMessage.dart';
import 'package:app/utils/debounce.dart';
import 'package:drag_and_drop_gridview/devdrag.dart';
import 'package:flutter/material.dart';

import '../globals.dart' as globals;
import 'dashboardWidget.dart';

class Dashboard extends StatefulWidget {
  final int pageId;
  final bool editMode;

  const Dashboard({Key key, this.pageId, this.editMode}) : super(key: key);

  @override
  DashboardState createState() => DashboardState();
}

class DashboardState extends State<Dashboard> {
  List<Module> modules = [];
  double spacing = 20;

  int crossAxisCount = 5;
  bool loading = true;

  double sizeOne = 100;
  Stream<dynamic> stream;
  StreamSubscription<dynamic> streamSubscription;

  List<DashboardWidget> modulesWidgets = [];
  ScrollController scrollController;
  Debouncer debouncer = new Debouncer(milliseconds: 500);

  // Map<int, StreamController<ModuleMessage>> messages = Map();

  @override
  void initState() {
    super.initState();
    print('dashboard page ${widget.pageId}');
    setState(() {
      sizeOne = 100 + spacing;
    });
    if (widget.pageId != null) {
      getLayout(true);
    }
  }

  @override
  void dispose() {
    streamSubscription.cancel();
    globals.service.closeWebSocket();

    super.dispose();
  }

  @override
  void didUpdateWidget(Dashboard oldDashboard) {
    super.didUpdateWidget(oldDashboard);
    if (oldDashboard.pageId != widget.pageId) {
      getLayout(true);
    }

    if (oldDashboard.editMode != widget.editMode) {
      print('editmode changed');
      setModules(context);
    }
  }

  void initStreams() {
    stream = globals.service.getWebsocketStream();

    // initializin the client status for the backend
    globals.service.sendWebsocketMessage(ModuleMessage(
        id: -1, command: 'changePage', message: widget.pageId.toString()));

    if (streamSubscription == null) {
      streamSubscription = stream.listen(propagateMessage);
    }
  }

  void propagateMessage(dynamic wsMessage) {
    var json = jsonDecode(wsMessage as String);
    ModuleMessage message = ModuleMessage.fromJson(json);

    var module =
        this.modulesWidgets.firstWhere((m) => m.getModuleId() == message.id);
    module.setLastMessage(message);
  }

  void setLayout() {
    // we init the streams then we set modules for the tile render
    initStreams();

    setModules(context);
    print("we have ${modulesWidgets.length} modules");
  }

  void getLayout(bool showLoading) async {
    if (showLoading) {
      setState(() {
        loading = true;
      });
    }
    List<Module> modules = await globals.service.getModules(widget.pageId);
    setState(() {
      this.modules = modules;
    });

    this.setLayout();

    setState(() {
      loading = false;
    });
  }

  Widget getModule(BuildContext context, int index) {
    return this.modulesWidgets[index];
  }

  void setModules(BuildContext context) {
    List<DashboardWidget> widgets = [];
    this.modules.asMap().forEach((index,module) {
      var dashboardWidget = DashboardWidget(
          key: Key(module.id.toString()),
          module: module,
          refreshLayout: getLayout,
          isLast: index == this.modules.length -1,
          pageId: widget.pageId,
          editMode: widget.editMode);
      widgets.add(dashboardWidget);
    });

    setState(() {
      this.modulesWidgets = widgets;
    });
  }

  void checkNeedGridResize(BuildContext context) {
    if (this.modules != null) {
      int _screenWidth = MediaQuery.of(context).size.width.floor();
      this.crossAxisCount = max((_screenWidth / 250).floor(), 1);
      print("${_screenWidth}px wide, ${this.crossAxisCount} items on grid");
    }
  }

  void reorderItem(oldIndex, newIndex) {}

  @override
  Widget build(BuildContext context) {
    checkNeedGridResize(context);

    if (loading) {
      return Container(
          child: Center(heightFactor: 1.0, child: CircularProgressIndicator()));
    }

    if (!this.loading &&
        this.modules != null) {
      return Container(
          // color: Colors.white,
          child: DragAndDropGridView(
        controller: scrollController,
        gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: this.crossAxisCount,
            crossAxisSpacing: 10,
            mainAxisSpacing: 10,
            childAspectRatio: 2),
        padding: EdgeInsets.all(20),
        itemBuilder: this.getModule,
        itemCount: this.modules.length,
        onWillAccept: (oldIndex, newIndex) => true,
        onReorder: this.reorderItem,
      ));
    } else {
      return Text('No layout for this screen size');
    }
  }
}
