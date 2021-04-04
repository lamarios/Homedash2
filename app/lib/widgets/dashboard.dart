import 'dart:async';
import 'dart:convert';

import 'package:app/model/moduleMessage.dart';
import 'package:app/model/pageLayout.dart';
import 'package:app/utils/debounce.dart';
import 'package:flutter/material.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';

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
  PageLayout pageLayout;
  double spacing = 20;

  int screenWidth;
  bool loading;

  double sizeOne = 100;
  Stream<dynamic> stream;
  StreamSubscription<dynamic> streamSubscription;

  List<DashboardWidget> modules = [];
  List<StaggeredTile> tiles = [];
  Debouncer debouncer = new Debouncer(milliseconds: 500);

  Map<int, StreamController<ModuleMessage>> messages = Map();

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

  void setLayout(PageLayout layout) {
    this.pageLayout = layout;
    // we init the streams then we set modules for the tile render
    initStreams();

    globals.service.sendWebsocketMessage(
        ModuleMessage(command: "changeLayout", message: layout.layout.id));
    setModules(context);
    print("we have ${modules.length} modules and ${tiles.length} tiles");
  }

  void getLayout(bool showLoading) async {
    if (showLoading) {
      setState(() {
        loading = true;
      });
    }
    var _screenWidth = MediaQuery.of(context).size.width;

    print("GETTING LAYOUT screenwidth = $_screenWidth");
    setState(() {
      screenWidth = _screenWidth.floor();
    });
    PageLayout pageLayout = await globals.service
        .getPageLayout(widget.pageId, _screenWidth.floor());

    setLayout(pageLayout);

    setState(() {
      loading = false;
    });
  }

  StaggeredTile getModuleSize(int index) {
    var layout = this.pageLayout.modules[index];
    var split = layout.size.split("x");
    int x = int.tryParse(split[0]);
    int y = int.parse(split[1]);
    return StaggeredTile.count(x, y);
  }

  setModules(BuildContext context) {
    List<DashboardWidget> widgets = [];
    List<StaggeredTile> tiles = [];
    this.pageLayout.modules.forEach((layout) {
      var stream = messages[layout.module.id];

      var dashboardWidget = DashboardWidget(
          key: Key(pageLayout.layout.maxGridWidth.toString() +
              '-' +
              layout.module.id.toString()),
          pageLayout: pageLayout,
          moduleLayout: layout,
          stream: stream,
          refreshLayout: getLayout,
          pageId: widget.pageId,
          editMode: widget.editMode);

      widgets.add(dashboardWidget);

      var split = layout.size.split("x");
      int x = int.tryParse(split[0]);
      int y = int.parse(split[1]);
      tiles.add(StaggeredTile.count(x, y));
    });

    setState(() {
      this.modules = widgets;
      this.tiles = tiles;
    });
  }

  checkNeedGridResize(BuildContext context) {
    if (this.pageLayout != null) {
      var _screenWidth = MediaQuery.of(context).size.width.floor();

      if (_screenWidth != this.screenWidth) {
        setState(() {
          loading = true;
        });
        debouncer.run(() {
          // calculating how many grid units we need
          var required = (_screenWidth / sizeOne).ceil();
          print(
              'required: $required current: ${pageLayout.layout.maxGridWidth}');
          getLayout(true);
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    checkNeedGridResize(context);
    // TODO: implement build

    if (loading) {
      return Container(
          child: Center(heightFactor: 1.0, child: CircularProgressIndicator()));
    }

    if (!this.loading &&
        this.pageLayout != null &&
        this.pageLayout.layout != null) {
      var gridWidth =
          this.pageLayout.layout.maxGridWidth.toDouble() * this.sizeOne;

      return Container(
          width: gridWidth,
          // color: Colors.white,
          child: StaggeredGridView.count(
            crossAxisSpacing: this.spacing,
            mainAxisSpacing: this.spacing,
            crossAxisCount: this.pageLayout.layout.maxGridWidth,
            staggeredTiles: tiles,
            children: modules,
          ));
/*
          child: StaggeredGridView.countBuilder(
              crossAxisSpacing: this.spacing,
              mainAxisSpacing: this.spacing,
              crossAxisCount: this.pageLayout.layout.maxGridWidth,
              itemCount: this.pageLayout.modules.length,
              itemBuilder: (BuildContext context, int index) {
                var layout = this.pageLayout.modules[index];
                var stream = messages[layout.module.id];

                return DashboardWidget(
                    key: Key(pageLayout.layout.maxGridWidth.toString() +
                        '-' +
                        layout.module.id.toString()),
                    pageLayout: pageLayout,
                    moduleLayout: layout,
                    stream: stream,
                    refreshLayout: getLayout,
                    pageId: widget.pageId,
                    editMode: widget.editMode);
              },
              staggeredTileBuilder: (int index) => getModuleSize(index)));
*/
    } else {
      return Text('No layout for this screen size');
    }
  }
}
