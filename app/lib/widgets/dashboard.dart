import 'package:app/model/pageLayout.dart';
import 'package:flutter/material.dart';

import '../globals.dart' as globals;

class Dashboard extends StatefulWidget {
  final int pageId;

  const Dashboard({Key key, this.pageId}) : super(key: key);

  @override
  _DashboardState createState() => _DashboardState();
}

class _DashboardState extends State<Dashboard> {
  PageLayout pageLayout;

  @override
  void initState() {
    getLayout();
  }

  void getLayout() async {
    PageLayout pageLayout = await globals.service.getPageLayout(1, 600);
    setState(() {
      this.pageLayout = pageLayout;
    });
  }

  @override
  Widget build(BuildContext context) {
    // TODO: implement build
    return Text(widget.pageId.toString());
  }
}
