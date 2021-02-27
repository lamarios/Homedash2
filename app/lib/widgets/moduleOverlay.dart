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

  ModuleOverlay(
      {this.selected, this.layout, this.pageLayout, this.refreshLayout});

  @override
  State<StatefulWidget> createState() => ModuleOverlayState();
}

class ModuleOverlayState extends State<ModuleOverlay>
    with TickerProviderStateMixin {
  int resizeArrowAngle;
  String nextSize;
  AnimationController animationController;
  Animation<double> animation;

  @override
  void initState() {
    super.initState();
    setNextModuleSize();
  }

  setNextModuleSize() async {
    String nextSize = await globals.service.getModuleNextAvailableSize(
        widget.layout.size,
        widget.layout.module.id,
        widget.pageLayout.layout.maxGridWidth);
    print(
        " max grid width ${widget.pageLayout.layout.maxGridWidth} nextSize: $nextSize");

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
    int angle;
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

    setState(() {
      resizeArrowAngle = angle;
      this.nextSize = nextSize;
      animationController = AnimationController(
          vsync: this,
          value: angle / 360,
          duration: const Duration(milliseconds: 500));
      animation = CurvedAnimation(
          parent: animationController, curve: Curves.elasticInOut);
    });
  }

  changeModuleSize() async {
    await globals.service.saveModuleSize(widget.layout.id, nextSize);
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
          Expanded(child: Container(child: Text('center'))),
          Row(
            children: [
              resizeArrowAngle != null
                  ? RotationTransition(
                      turns: animation,
                      child: IconButton(
                        onPressed: changeModuleSize,
                        icon: FaIcon(
                          FontAwesomeIcons.arrowRight,
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
