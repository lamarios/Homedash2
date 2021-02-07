import 'package:app/model/page.dart';
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
                  child: Column(children: <Widget>[
                    Container(height: 75, child: Text('Pages')),
                    Expanded(
                        child: ListView.builder(
                      itemCount: pages.length,
                      scrollDirection: Axis.vertical,
                      shrinkWrap: false,
                      itemBuilder: (context, index) {
                        return Container(
                            decoration: BoxDecoration(
                                color: Colors.white,
                                borderRadius:
                                    BorderRadius.all(Radius.circular(10)),
                                boxShadow: [
                                  BoxShadow(
                                    color: Colors.grey.withOpacity(0.5),
                                    spreadRadius: 2,
                                    blurRadius: 5,
                                    offset: Offset(
                                        2, 2), // changes position of shadow
                                  ),
                                ]),
                            child: Padding(
                                padding: const EdgeInsets.all(20),
                                child: Text('Page ${pages[index].name}')));
                      },
                    ))
                  ]))),
          Expanded(child: Text('yo'))
        ],
      ),
    );
  }
}
