import 'dart:async';
import 'dart:convert';

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
  final double spacing = 15;
  final double sizeOne = 100;
  Stream<dynamic> stream;
  StreamSubscription<dynamic> streamSubscription;

  Map<int, StreamController<ModuleMessage>> messages = Map();

  @override
  void initState() {
    getLayout();
  }

  @override
  void dispose() {
    streamSubscription.cancel();
    globals.service.closeWebSocket();
    super.dispose();
  }

  void initStreams() {
    print("${messages.length} streams created");

    Map<int, StreamController<ModuleMessage>> streams = Map();
    pageLayout.modules.forEach((element) {
      streams[element.module.id] = StreamController<ModuleMessage>();
    });

    setState(() {
      this.messages = streams;
      print(streams.toString());
    });

    stream = globals.service.getWebsocketStream();
    streamSubscription = stream.listen(propagateMessage);

    // initializin the client status for the backend
    globals.service.sendWebsocketMessage(ModuleMessage(
        id: -1, command: 'changePage', message: widget.pageId.toString()));
    globals.service.sendWebsocketMessage(ModuleMessage(
        id: -1,
        command: 'changeLayout',
        message: this.pageLayout.layout.id.toString()));
  }

  void propagateMessage(dynamic wsMessage) {
    var json = jsonDecode(wsMessage as String);
    ModuleMessage message = ModuleMessage.fromJson(json);

    messages[message.id].add(message);
  }

  void getLayout() async {
    PageLayout pageLayout =
        await globals.service.getPageLayout(widget.pageId, 600);
    // setState(() {
    this.pageLayout = pageLayout;
    // });
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
              itemBuilder: (BuildContext context, int index) {
                var layout = this.pageLayout.modules[index];
                var stream = messages[layout.module.id];

                return DashboardWidget(moduleLayout: layout, stream: stream);
              },
              staggeredTileBuilder: (int index) => getModuleSize(index)));
    } else {
      return Text('No layout for this screen size');
    }
  }
}
