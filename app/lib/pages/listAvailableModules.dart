import 'package:app/model/plugin.dart';
import 'package:app/pages/addUpdateModule.dart';
import 'package:flutter/material.dart';

import '../globals.dart' as globals;

class AvailableModules extends StatefulWidget {
  int pageId;

  AvailableModules({this.pageId});

  @override
  State<AvailableModules> createState() => AvailableModulesState();
}

class AvailableModulesState extends State<AvailableModules> {
  List<Plugin> plugins = [];

  @override
  void initState() {
    getAvailablePlugins();
  }

  void getAvailablePlugins() async {
    var plugins = await globals.service.getAvailablePlugins();

    setState(() {
      this.plugins = plugins;
    });
  }

  /// if the plugin has settings we redirect the user to the settings page
  addModule(Plugin plugin) {
    if (plugin.settings) {
      Navigator.push(
          context,
          MaterialPageRoute(
              builder: (context) =>
                  AddUpdateModule(plugin: plugin, pageId: widget.pageId)));
    } else {
      // TODO: save plugin and redirect to main page
    }
  }

  @override
  Widget build(BuildContext context) {
    var _screenWidth = MediaQuery.of(context).size.width;

    var crossAxisCount = 3;
    if (_screenWidth < 700) {
      crossAxisCount = 1;
    } else if (_screenWidth < 1000) {
      crossAxisCount = 2;
    } else if (_screenWidth < 1500) {
      crossAxisCount = 3;
    } else if (_screenWidth < 2000) {
      crossAxisCount = 4;
    } else {
      crossAxisCount = 5;
    }

    return Scaffold(
        appBar: AppBar(
          // Here we take the value from the MyHomePage object that was created by
          // the App.build method, and use it to set our appbar title.
          title: Text('Add module'),
        ),
        body: GridView.builder(
            padding: EdgeInsets.all(20),
            gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                childAspectRatio: 3,
                crossAxisCount: crossAxisCount,
                crossAxisSpacing: 20,
                mainAxisSpacing: 20),
            itemCount: plugins.length,
            itemBuilder: (BuildContext context, int index) {
              var plugin = plugins[index];
              return Card(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                    Padding(
                        padding: EdgeInsets.all(10),
                        child: Text(plugin.displayName,style: TextStyle(
                          color: Theme.of(context).accentColor,
                          fontWeight: FontWeight.bold
                        ),)),
                    Expanded(
                        child: Padding(
                            padding: EdgeInsets.all(10),
                            child: Text(plugin.description))),
                    Row(
                      children: [
                        Expanded(child: SizedBox.shrink()),
                        TextButton(
                            onPressed: () => addModule(plugin),
                            child: Text(plugin.settings ? 'Configure' : 'Add'))
                      ],
                    )
                  ]));
            }));
  }
}
