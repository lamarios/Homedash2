import 'package:app/model/page.dart';
import 'package:app/widgets/dashboard.dart';
import 'package:app/widgets/leftMenu.dart';
import 'package:flutter/material.dart';

import '../globals.dart' as globals;

class MainPage extends StatefulWidget {
  @override
  _MainPageState createState() => _MainPageState();
}

class _MainPageState extends State<MainPage> {
  List<PluginPage> pages = <PluginPage>[];

  @override
  void initState() {
    getPages();
  }

  getPages() async {
    final serverPages = await globals.service.getPages();
    setState(() {
      this.pages = serverPages;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Row(
        children: <Widget>[
          Container(
              color: Theme.of(context).primaryColorLight,
              width: 300,
              child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: LeftMenu(pages: this.pages))),
           Container(child: Dashboard(pageId: 1))
        ],
      ),
    );
  }
}
