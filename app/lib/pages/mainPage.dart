import 'package:app/model/page.dart';
import 'package:app/utils/preferences.dart';
import 'package:app/widgets/dashboard.dart';
import 'package:app/widgets/leftMenu.dart';
import 'package:flutter/material.dart';

import '../globals.dart' as globals;

const EDIT_LAYOUT = "edit-layout";

class MainPage extends StatefulWidget {
  @override
  MainPageState createState() => MainPageState();
}

class MainPageState extends State<MainPage> {
  List<PluginPage> pages = <PluginPage>[];
  int currentPage;
  final scaffoldKey = GlobalKey<ScaffoldState>();
  static final dashboardKey = GlobalKey<DashboardState>();
  bool editMode = false;

  @override
  void initState() {
    Preferences.getInt(Preferences.CURRENT_PAGE).then((page) {
      setState(() {
        this.currentPage = page;
      });
      getPages();
    });
  }

  popUpSelect(String s) {
    print("Selected $s");
    switch (s) {
      case EDIT_LAYOUT:
        setState(() {
          editMode = !editMode;
        });
        break;
    }
  }

  getPages() async {
    final serverPages = await globals.service.getPages();
    setPages(serverPages);
  }

  setPages(List<PluginPage> pages) {
    var page =
        pages.firstWhere((p) => p.id == this.currentPage, orElse: () => null);
    print('set Page, current page: $page');
    setState(() {
      if (this.currentPage == null || page == null) {
        this.currentPage = pages[0].id;
      }
      this.pages = pages;
    });
  }

  changePage(int id) {
    print('Changing page to: $id');
    Preferences.setInt(Preferences.CURRENT_PAGE, id);
    Navigator.of(context).pop();
    setState(() {
      this.currentPage = id;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: scaffoldKey,
      drawer: Container(
          padding: EdgeInsets.all(20),
          color: Colors.white,
          width: 300,
          child: LeftMenu(
            pages: this.pages,
            changePage: this.changePage,
            setPages: this.setPages,
            currentPage: this.currentPage,
          )),
      appBar: AppBar(
        automaticallyImplyLeading: false,
        leading: IconButton(
          icon: Image.asset('images/logo.png'),
          onPressed: () {
            scaffoldKey.currentState.openDrawer();
          },
        ),
        backgroundColor: Colors.white,
        actions: [
          PopupMenuButton<String>(
              onSelected: this.popUpSelect,
              itemBuilder: (itemBuilder) => [
                    PopupMenuItem(
                        value: EDIT_LAYOUT, child: Text('Edit Layout'))
                  ])
        ],
      ),
      body: Center(
          child: Padding(
              padding: const EdgeInsets.all(20),
              child: Dashboard(
                  key: dashboardKey,
                  pageId: this.currentPage,
                  editMode: editMode))),
    );
  }
}
