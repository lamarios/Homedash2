import 'dart:async';

import 'package:app/model/moduleMessage.dart';
import 'package:app/model/pageLayout.dart';
import 'package:app/widgets/dashboardWidget.dart';
import 'package:flutter/material.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';

import '../globals.dart' as globals;

class Dashboard extends StatefulWidget {
  final int pageId;

  const Dashboard({Key key, this.pageId}) : super(key: key);

  @override
  _DashboardState createState() => _DashboardState();
}

class _DashboardState extends State<Dashboard> {
  PageLayout pageLayout;
  final double spacing = 10;
  final double sizeOne = 100;

  Map<int, StreamController<ModuleMessage>> messages = Map();

  @override
  void initState() {
    getLayout();
  }

  void initStreams() {
    this.pageLayout.modules.forEach((module) {
      messages.putIfAbsent(module.id, () => StreamController<ModuleMessage>());
    });

    print("${messages.length} streams created");

    globals.service.getWebsocketStream().listen(propagateMessage);
  }

  void propagateMessage(dynamic wsMessage) {
    print(wsMessage as String);
  }

  void getLayout() async {
    PageLayout pageLayout =
        await globals.service.getPageLayout(widget.pageId, 600);
    setState(() {
      this.pageLayout = pageLayout;
    });
    // now we init the message stream for each module
    initStreams();
  }

  StaggeredTile getModuleSize(int index) {
    var layout = this.pageLayout.modules[index];
    var split = layout.size.split("x");
    int x = int.tryParse(split[0]);
    int y = int.parse(split[1]);
    return StaggeredTile.count(x, y);
  }

  @override
  Widget build(BuildContext context) {
    // TODO: implement build
    if (this.pageLayout != null && this.pageLayout.layout != null) {
      var gridWidth =
          this.pageLayout.layout.maxGridWidth.toDouble() * this.sizeOne;

      return Container(
          width: gridWidth,
          child: StaggeredGridView.countBuilder(
              crossAxisSpacing: this.spacing,
              mainAxisSpacing: this.spacing,
              crossAxisCount: this.pageLayout.layout.maxGridWidth,
              itemCount: this.pageLayout.modules.length,
              itemBuilder: (BuildContext context, int index) =>
                  DashboardWidget(moduleLayout: this.pageLayout.modules[index]),
              staggeredTileBuilder: (int index) => getModuleSize(index)));
    } else {
      return Text('No layout for this screen size');
    }
  }
}
