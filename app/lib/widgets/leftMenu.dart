import 'package:app/model/page.dart';
import 'package:app/pages/listAvailableModules.dart';
import 'package:flutter/material.dart';

class LeftMenu extends StatefulWidget {
  List<PluginPage> pages;

  LeftMenu({this.pages});

  @override
  State<StatefulWidget> createState() => LeftMenuState();
}

class LeftMenuState extends State<LeftMenu> {
  void goToPluginList() {
    Navigator.push(context,
        MaterialPageRoute(builder: (context) => AvailableModules(pageId: 1)));
  }

  @override
  Widget build(BuildContext context) {
    return Column(children: <Widget>[
      Container(height: 75, child: Text('Pages')),
      Expanded(
          child: ListView.builder(
        itemCount: widget.pages.length,
        scrollDirection: Axis.vertical,
        shrinkWrap: false,
        itemBuilder: (context, index) {
          return Container(
              decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.all(Radius.circular(10)),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.grey.withOpacity(0.5),
                      spreadRadius: 2,
                      blurRadius: 5,
                      offset: Offset(2, 2), // changes position of shadow
                    ),
                  ]),
              child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Text('Page ${widget.pages[index].name}')));
        },
      )),
      TextButton(child: Text('Add module'), onPressed: goToPluginList)
    ]);
  }
}
