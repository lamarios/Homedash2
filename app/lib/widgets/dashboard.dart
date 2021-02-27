import 'dart:async';
import 'dart:convert';

import 'package:app/model/moduleMessage.dart';
import 'package:app/model/pageLayout.dart';
import 'package:flutter/material.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';

import '../globals.dart' as globals;
import 'dashboardWidget.dart';

class Dashboard extends StatefulWidget {
  final int pageId;
  final bool editMode;

  const Dashboard({Key key, this.pageId, this.editMode}) : super(key: key);

  @override
  _DashboardState createState() => _DashboardState();
}

class _DashboardState extends State<Dashboard> {
  PageLayout pageLayout;
  double spacing = 20;

  double sizeOne = 100;
  Stream<dynamic> stream;
  StreamSubscription<dynamic> streamSubscription;

  List<DashboardWidget> modules = [];
  List<StaggeredTile> tiles = [];

  Map<int, StreamController<ModuleMessage>> messages = Map();

  @override
  void initState() {
    super.initState();
    print('dashboard page ${widget.pageId}');
    setState(() {
      sizeOne = 100 + spacing;
    });
    if (widget.pageId != null) {
      getLayout();
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
      getLayout();
    }

    if (oldDashboard.editMode != widget.editMode) {
      print('editmode changed');
      // getModules(context);
    }
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

    // initializin the client status for the backend
    globals.service.sendWebsocketMessage(ModuleMessage(
        id: -1, command: 'changePage', message: widget.pageId.toString()));
    globals.service.sendWebsocketMessage(ModuleMessage(
        id: -1,
        command: 'changeLayout',
        message: this.pageLayout.layout.id.toString()));

    if (streamSubscription == null) {
      streamSubscription = stream.listen(propagateMessage);
    }
  }

  void propagateMessage(dynamic wsMessage) {
    var json = jsonDecode(wsMessage as String);
    ModuleMessage message = ModuleMessage.fromJson(json);
    messages[message.id].add(message);
  }

  void swapModules(int oldIndex, int newIndex) async {
    print("${oldIndex} => ${newIndex}");

    var old = this.pageLayout.modules;
    old.insert(newIndex, old.removeAt(oldIndex));

    for (int i = 0; i < old.length; i++) {
      old[i].x = i;
    }

    var layouts = await globals.service.setPageLayout(widget.pageId, 600, old);

    setLayout(layouts);
  }

  void setLayout(PageLayout layout) {
    this.pageLayout = layout;
    // we init the streams then we set modules for the tile render
    initStreams();
    // getModules(context);
    // getTiles();
    print("we have ${modules.length} modules and ${tiles.length} tiles");
  }

  void getLayout() async {
    print("GETTING LAYOUT");
    PageLayout pageLayout =
        await globals.service.getPageLayout(widget.pageId, 600);

    setLayout(pageLayout);
  }

  StaggeredTile getModuleSize(int index){
    var layout = this.pageLayout.modules[index];
    var split = layout.size.split("x");
    int x = int.tryParse(split[0]);
    int y = int.parse(split[1]);
    return StaggeredTile.count(x, y);
  }

  getTiles() {
    print('Getting tiles');
    List<StaggeredTile> tiles = [];
    this.pageLayout.modules.forEach((layout) {
      var split = layout.size.split("x");
      int x = int.tryParse(split[0]);
      int y = int.parse(split[1]);
      tiles.add(StaggeredTile.count(x, y));
    });

    setState(() {
      this.tiles = tiles;
    });
  }

  getModules(BuildContext context) {
    List<DashboardWidget> widgets = [];
    this.pageLayout.modules.forEach((layout) {
      var stream = messages[layout.module.id];

      widgets.add(DashboardWidget(
          key: Key(layout.id.toString()),
          pageLayout: pageLayout,
          moduleLayout: layout,
          stream: stream,
          refreshLayout: getLayout,
          editMode: widget.editMode));
    });
    setState(() {
      this.modules = widgets;
    });
  }

  @override
  Widget build(BuildContext context) {
    // TODO: implement build
    if (this.pageLayout != null && this.pageLayout.layout != null) {
      var gridWidth =
          this.pageLayout.layout.maxGridWidth.toDouble() * this.sizeOne;

      return Container(
          width: gridWidth,
          color: Colors.white,
          /*      child: ReorderableItemsView(
            children: modules,
            staggeredTiles: tiles,
            isGrid: true,
            crossAxisSpacing: this.spacing,
            mainAxisSpacing: this.spacing,
            crossAxisCount: this.pageLayout.layout.maxGridWidth,
            onReorder: swapModules,
          )*/

          child: StaggeredGridView.countBuilder(
              crossAxisSpacing: this.spacing,
              mainAxisSpacing: this.spacing,
              crossAxisCount: this.pageLayout.layout.maxGridWidth,
              itemCount: this.pageLayout.modules.length,
              itemBuilder: (BuildContext context, int index) {
                var layout = this.pageLayout.modules[index];
                var stream = messages[layout.module.id];

                return DashboardWidget(
                    pageLayout: pageLayout,
                    moduleLayout: layout,
                    stream: stream,
                    refreshLayout: getLayout,
                    editMode: widget.editMode);
              },
              staggeredTileBuilder: (int index) => getModuleSize(index))
          );
    } else {
      return Text('No layout for this screen size');
    }
  }
}
