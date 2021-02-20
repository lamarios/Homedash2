import 'package:app/globals.dart' as globals;
import 'package:app/model/page.dart';
import 'package:app/pages/listAvailableModules.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';

class LeftMenu extends StatefulWidget {
  List<PluginPage> pages;
  Function changePage, setPages;
  int currentPage;

  LeftMenu({this.pages, this.currentPage, this.changePage, this.setPages});

  @override
  State<StatefulWidget> createState() => LeftMenuState();
}

class LeftMenuState extends State<LeftMenu> {
  final TextEditingController boardNameController = TextEditingController();
  bool boardEditMode = false;

  void goToPluginList() {
    Navigator.push(
        context,
        MaterialPageRoute(
            builder: (context) =>
                AvailableModules(pageId: widget.currentPage)));
  }

  addPage() async {
    String pageName = boardNameController.text;
    print('new page name: ${pageName}');

    final pages = await globals.service.addPage(pageName);
    widget.setPages(pages);

    Navigator.of(context).pop();
  }

  renamePage(int id) async {
    String pageName = boardNameController.text;
    print('page ${id} new name name: ${pageName}');

    final pages = await globals.service.renamePage(id, pageName);
    widget.setPages(pages);
    Navigator.of(context).pop();
  }

  deletePage(int id) async {
    final pages = await globals.service.deletePage(id);
    widget.setPages(pages);
    Navigator.of(context).pop();
  }

  showAddPageDialog() {
    boardNameController.text = "";
    return showDialog<void>(
      context: context,
      barrierDismissible: false, // user must tap button!
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text('Add page'),
          content: SingleChildScrollView(
            child: ListBody(
              children: <Widget>[
                Text('New board name'),
                TextFormField(
                  controller: boardNameController,
                  validator: (value) {
                    if (value.isEmpty) {
                      return 'Please enter some text';
                    }
                    return null;
                  },
                )
              ],
            ),
          ),
          actions: <Widget>[
            TextButton(
              child: Text('Cancel'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            TextButton(
              child: Text('Add'),
              onPressed: addPage,
            ),
          ],
        );
      },
    );
  }

  showChangePageNameDialog(PluginPage page) {
    boardNameController.text = "";
    return showDialog<void>(
      context: context,
      barrierDismissible: false, // user must tap button!
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text('Rename board ${page.name}'),
          content: SingleChildScrollView(
            child: ListBody(
              children: <Widget>[
                Text('New name'),
                TextFormField(
                  controller: boardNameController,
                  validator: (value) {
                    if (value.isEmpty) {
                      return 'Please enter some text';
                    }
                    return null;
                  },
                )
              ],
            ),
          ),
          actions: <Widget>[
            TextButton(
              child: Text('Cancel'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            TextButton(
              child: Text('Rename'),
              onPressed: () => renamePage(page.id),
            ),
          ],
        );
      },
    );
  }

  showDeletePageDialog(PluginPage page) {
    return showDialog<void>(
      context: context,
      barrierDismissible: false, // user must tap button!
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text('Delete board ${page.name}'),
          content: Text('This will delete the board and all the modues on it'),
          actions: <Widget>[
            TextButton(
              child: Text('Cancel'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            TextButton(
              child: Text('Ok'),
              onPressed: () => deletePage(page.id),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Column(children: <Widget>[
      Row(children: [
        Image.asset("images/logo.png", width: 40, height: 40),
        Text(
          'omedash',
          style: TextStyle(fontSize: 20),
        )
      ]),
      Padding(
          padding: EdgeInsets.only(top: 20),
          child: Row(children: [
            FaIcon(
              FontAwesomeIcons.clone,
              size: 15,
            ),
            Expanded(
                child: Padding(
                    padding: EdgeInsets.all(5),
                    child: Text('Boards', style: TextStyle(fontSize: 20)))),
            IconButton(
              icon: FaIcon(FontAwesomeIcons.plus),
              onPressed: showAddPageDialog,
              iconSize: 15,
            ),
            IconButton(
              icon: FaIcon(FontAwesomeIcons.pencilAlt),
              onPressed: () {
                setState(() {
                  this.boardEditMode = !this.boardEditMode;
                });
              },
              color: this.boardEditMode
                  ? Theme.of(context).accentColor
                  : Theme.of(context).textTheme.bodyText1.color,
              iconSize: 15,
            ),
          ])),
      Expanded(
          child: ListView.builder(
        itemCount: widget.pages.length,
        scrollDirection: Axis.vertical,
        shrinkWrap: false,
        itemBuilder: (context, index) {
          PluginPage page = widget.pages[index];
          bool selected = page.id == widget.currentPage;

          return GestureDetector(
              onTap: () {
                widget.changePage(page.id);
              },
              child: Container(
                  margin: EdgeInsets.only(left: 20),
                  decoration: selected
                      ? BoxDecoration(
                          color: Theme.of(context).accentColor,
                          borderRadius:
                              new BorderRadius.all(Radius.circular(5)))
                      : BoxDecoration(color: Color.fromRGBO(255, 255, 255, 0)),
                  child: Padding(
                      padding: const EdgeInsets.all(5),
                      child: Row(children: [
                        Expanded(
                          child: Text(
                            '${page.name}',
                            style: TextStyle(
                                fontSize: 20,
                                color: selected
                                    ? Colors.white
                                    : Theme.of(context)
                                        .textTheme
                                        .bodyText1
                                        .color),
                          ),
                        ),
                        boardEditMode
                            ? IconButton(
                                icon: FaIcon(FontAwesomeIcons.pencilAlt),
                                iconSize: 15,
                                color: selected
                                    ? Colors.white
                                    : Theme.of(context)
                                        .textTheme
                                        .bodyText1
                                        .color,
                                onPressed: () => showChangePageNameDialog(page))
                            : SizedBox.shrink(),
                        boardEditMode && page.id != 1
                            ? IconButton(
                                icon: FaIcon(FontAwesomeIcons.times),
                                iconSize: 15,
                                color: selected
                                    ? Colors.white
                                    : Theme.of(context)
                                        .textTheme
                                        .bodyText1
                                        .color,
                                onPressed: () => showDeletePageDialog(page))
                            : SizedBox.shrink(),
                      ]))));
        },
      )),
      TextButton(child: Text('Add module'), onPressed: goToPluginList)
    ]);
  }
}
