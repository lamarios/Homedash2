import 'package:app/globals.dart' as globals;
import 'package:app/model/module.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';

class ModuleOverlay extends StatefulWidget {
  bool selected;
  Module module;
  Function refreshLayout;
  int pageId;
  bool isLast;

  ModuleOverlay(
      {this.selected,
      this.module,
      this.refreshLayout,
      this.pageId,
      this.isLast});

  @override
  State<StatefulWidget> createState() => ModuleOverlayState();
}

class ModuleOverlayState extends State<ModuleOverlay>
    with SingleTickerProviderStateMixin {
  double resizeArrowAngle;
  AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this, // the SingleTickerProviderStateMixin
      duration: Duration(milliseconds: 500),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    print('disposing');
    super.dispose();
  }

  moveModule(bool forward) async {
    await globals.service.moveModule(widget.module.id, forward, widget.pageId);
    widget.refreshLayout(false);
  }

  deleteModule() async {
    await globals.service.deleteModule(widget.module.id);
    widget.refreshLayout(false);
  }

  showDeleteDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text('Delete module ?'),
          content: Text("Are You Sure Want To Proceed ?"),
          actions: <Widget>[
            TextButton(
              child: Text("YES"),
              onPressed: () {
                //Put your code here which you want to execute on Yes button click.
                deleteModule();
                Navigator.of(context).pop();
              },
            ),
            TextButton(
              child: Text("NO"),
              onPressed: () {
                //Put your code here which you want to execute on No button click.
                Navigator.of(context).pop();
              },
            ),
          ],
        );
      },
    );
  }

  @override
  void didUpdateWidget(covariant ModuleOverlay oldWidget) {
    super.didUpdateWidget(oldWidget);
    // print("${oldWidget.layout.size} => ${widget.layout.size}");
  }

  @override
  Widget build(BuildContext context) {
    double iconSize = 15;
    return Container(
      color: Theme.of(context).accentColor.withOpacity(0.8),
      child: Column(
        children: [
          Row(
            children: [
              Text('title'),
              IconButton(
                icon: FaIcon(FontAwesomeIcons.times),
                onPressed: () => showDeleteDialog(context),
              )
            ],
          ),
          Expanded(
              child: Row(
            children: [
              widget.module.order > 0
                  ? IconButton(
                      iconSize: iconSize,
                      icon: FaIcon(
                        FontAwesomeIcons.arrowLeft,
                        color: Colors.white,
                        size: iconSize,
                      ),
                      onPressed: () => moveModule(false))
                  : SizedBox.shrink(),
              Expanded(child: SizedBox.shrink()),
              !widget.isLast
                  ? IconButton(
                      iconSize: iconSize,
                      icon: FaIcon(FontAwesomeIcons.arrowRight,
                          color: Colors.white),
                      onPressed: () => moveModule(true))
                  : SizedBox.shrink()
            ],
          )),
        ],
      ),
    );
  }
}
