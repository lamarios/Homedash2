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
    return Scaffold(
        appBar: AppBar(
          // Here we take the value from the MyHomePage object that was created by
          // the App.build method, and use it to set our appbar title.
          title: Text('Add module'),
        ),
        body: Column(
          children: [
            Text('Select the module to add'),
            Expanded(
                child: ListView.builder(
                    itemCount: plugins.length,
                    itemBuilder: (BuildContext context, int index) {
                      var plugin = plugins[index];
                      return Column(
                        children: [
                          Text(plugin.displayName),
                          Text(plugin.description),
                          TextButton(
                              onPressed: () => addModule(plugin),
                              child:
                                  Text(plugin.settings ? 'Configure' : 'Add'))
                        ],
                      );
                    }))
          ],
        ));
  }
}
