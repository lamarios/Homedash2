import 'dart:math';

import 'package:app/globals.dart' as globals;
import 'package:app/model/moduleLayout.dart';
import 'package:app/model/pageLayout.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';

class ModuleOverlay extends StatefulWidget {
  bool selected;
  ModuleLayout layout;

  PageLayout pageLayout;
  Function refreshLayout;
  int pageId;

  ModuleOverlay(
      {this.selected,
      this.layout,
      this.pageLayout,
      this.refreshLayout,
      this.pageId});

  @override
  State<StatefulWidget> createState() => ModuleOverlayState();
}

class ModuleOverlayState extends State<ModuleOverlay>
    with SingleTickerProviderStateMixin {
  double resizeArrowAngle;
  String nextSize;
  AnimationController _controller;
  int maxX = 0;

  @override
  void initState() {
    widget.pageLayout.modules.forEach((element) => maxX = max(element.x, maxX));

    super.initState();
    setNextModuleSize();
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

  setNextModuleSize() async {
    String nextSize = await globals.service.getModuleNextAvailableSize(
        widget.layout.size,
        widget.layout.module.id,
        widget.pageLayout.layout.maxGridWidth);

    List<String> split = nextSize.split('x');
    int newWidth = int.parse(split[0]);
    int newHeight = int.parse(split[1]);

    split = widget.layout.size.split('x');
    int currentWidth = int.parse(split[0]);
    int currentHeight = int.parse(split[1]);

    int widthDiff = (newWidth > currentWidth)
        ? 1
        : (newWidth == currentWidth)
            ? 0
            : -1;
    int heightDiff = (newHeight > currentHeight)
        ? 1
        : (newHeight == currentHeight)
            ? 0
            : -1;

    //base chevron direction is right (so already 90 degrees set)
    double angle;
    if (widthDiff == 0 && heightDiff == 0) {
      angle = null;
    } else {
      if (widthDiff == 1) {
        if (heightDiff == 1) {
          angle = 45;
        } else if (heightDiff == 0) {
          angle = 0;
        } else if (heightDiff == -1) {
          angle = -45;
        }
      } else if (widthDiff == 0) {
        if (heightDiff == 1) {
          angle = 90;
        } else if (heightDiff == 0) {
          angle = 0;
        } else if (heightDiff == -1) {
          angle = -90;
        }
      } else if (widthDiff == -1) {
        if (heightDiff == 1) {
          angle = 135;
        } else if (heightDiff == 0) {
          angle = 180;
        } else if (heightDiff == -1) {
          angle = 225;
        }
      }
    }

    _controller.animateTo(angle / 360,
        duration: Duration(milliseconds: 250), curve: Curves.fastOutSlowIn);

    setState(() {
      resizeArrowAngle = angle / 360;
      this.nextSize = nextSize;
    });
  }

  changeModuleSize() async {
    await globals.service.saveModuleSize(widget.layout.id, nextSize);
    widget.refreshLayout();
  }

  moveModule(bool forward) async {
    await globals.service.moveModule(widget.layout.id, forward, widget.pageId);
    widget.refreshLayout();
  }

  @override
  void didUpdateWidget(covariant ModuleOverlay oldWidget) {
    super.didUpdateWidget(oldWidget);
    // print("${oldWidget.layout.size} => ${widget.layout.size}");
    if (oldWidget.layout != null &&
        oldWidget.layout.size != widget.layout.size) {
      setNextModuleSize();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Theme.of(context).accentColor.withOpacity(0.8),
      child: Column(
        children: [
          Row(
            children: [Text('title')],
          ),
          Expanded(
              child: Row(
            children: [
              widget.layout.x > 0
                  ? IconButton(
                      icon: FaIcon(FontAwesomeIcons.arrowLeft,
                          color: Colors.white),
                      onPressed: () => moveModule(false))
                  : SizedBox.shrink(),
              Expanded(child: SizedBox.shrink()),
              widget.layout.x < maxX
                  ? IconButton(
                      icon: FaIcon(FontAwesomeIcons.arrowRight,
                          color: Colors.white),
                      onPressed: () => moveModule(true))
                  : SizedBox.shrink()
            ],
          )),
          Row(
            children: [
              Expanded(child: SizedBox.shrink()),
              resizeArrowAngle != null
                  ? RotationTransition(
                      turns: _controller,
                      child: IconButton(
                        onPressed: changeModuleSize,
                        icon: FaIcon(
                          FontAwesomeIcons.chevronRight,
                          color: Colors.white,
                        ),
                      ),
                    )
                  : SizedBox.shrink()
            ],
          ),
        ],
      ),
    );
  }
}
