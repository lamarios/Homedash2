import 'package:app/pages/mainPage.dart';
import 'package:app/service.dart';
import 'package:app/utils/preferences.dart';
import 'package:flutter/material.dart';

import 'globals.dart' as globals;
import 'model/ServerConfig.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // Try running your application with "flutter run". You'll see the
        // application has a blue toolbar. Then, without quitting the app, try
        // changing the primarySwatch below to Colors.green and then invoke
        // "hot reload" (press "r" in the console where you ran "flutter run",
        // or simply save your changes to "hot reload" in a Flutter IDE).
        // Notice that the counter didn't reset back to zero; the application
        // is not restarted.
        primarySwatch: Colors.deepOrange,
        backgroundColor: Colors.white,
      ),
      home: MyHomePage(title: 'Flutter demo  yo Home  Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  final _formKey = GlobalKey<FormState>();
  String url = "";
  String username;
  String password;
  ServerConfig config = ServerConfig(useAuth: false);
  TextEditingController urlController = TextEditingController();

  @override
  void initState() {
    super.initState();
    getServerUrl();
  }

  getServerUrl() async {
    final value = await Preferences.get(Preferences.SERVER_URL);
    print('read: $value');

    globals.service = Service(value);

    if (value != "") {
      urlController.text = value;

      config = await globals.service.getConfig();
      if (config.useAuth) {
        final token = await Preferences.get(Preferences.TOKEN);

        if (token != null && !Service.isTokenExpired(token)) {
          await globals.service.setToken(token);
          goToDashboard();
        }
      } else {
        goToDashboard();
      }
    }
  }

  saveServerUrl(value) async {
    Preferences.set(Preferences.SERVER_URL, value);
    print('saved $value');

    globals.service = Service(value);

    try {
      ServerConfig serverConfig = await globals.service.getConfig();
      setState(() {
        config = serverConfig;
      });

      if (!serverConfig.useAuth) {
        goToDashboard();
      }
    } catch (e) {
      _showMyDialog(e);
    }
  }

  Future<void> _showMyDialog(e) async {
    return showDialog<void>(
      context: context,
      barrierDismissible: false, // user must tap button!
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text('Error'),
          content: SingleChildScrollView(
            child: ListBody(
              children: <Widget>[
                Text(e.toString()),
              ],
            ),
          ),
          actions: <Widget>[
            TextButton(
              child: Text('Ok'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
          ],
        );
      },
    );
  }

  login() async {
    try {
      final success = await globals.service.login(username, password);
      if (success) {
        goToDashboard();
      }
    } catch (e) {
      _showMyDialog(e);
    }
  }

  goToDashboard() {
    Navigator.push(
        context, MaterialPageRoute(builder: (context) => MainPage()));
  }

  @override
  Widget build(BuildContext context) {
    final widgets = <Widget>[
      TextFormField(
        enabled: !config.useAuth,
        enableSuggestions: false,
        autocorrect: false,
        controller: urlController,
        decoration: const InputDecoration(
          hintText: 'Enter your Homedash server URL',
        ),
        validator: (value) {
          if (value.isEmpty) {
            return 'Please enter some text';
          }

          if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return 'Please enter a URL';
          }
          return null;
        },
        onSaved: saveServerUrl,
      ),
      Padding(
        padding: const EdgeInsets.symmetric(vertical: 16.0),
        child: ElevatedButton(
          onPressed: () {
            // Validate will return true if the form is valid, or false if
            // the form is invalid.
            if (_formKey.currentState.validate()) {
              // Process data.
              _formKey.currentState.save();
              if (config.useAuth) {
                login();
              }
            }
          },
          child: Text('Submit'),
        ),
      ),
    ];

    if (config.useAuth) {
      widgets.insertAll(1, <Widget>[
        TextFormField(
          decoration: const InputDecoration(
            hintText: 'Username',
          ),
          enableSuggestions: false,
          autocorrect: false,
          validator: (value) {
            if (value.isEmpty) {
              return 'Username required';
            }

            return null;
          },
          onSaved: (value) {
            username = value;
          },
        ),
        TextFormField(
          decoration: const InputDecoration(
            hintText: 'Password',
          ),
          enableSuggestions: false,
          autocorrect: false,
          obscureText: true,
          validator: (value) {
            if (value.isEmpty) {
              return 'Password required';
            }

            return null;
          },
          onSaved: (value) {
            password = value;
          },
        ),
      ]);
    }

    // This method is rerun every time setState is called, for instance as done
    // by the _incrementCounter method above.
    //
    // The Flutter framework has been optimized to make rerunning build methods
    // fast, so that you can just rebuild anything that needs updating rather
    // than having to individually change instances of widgets.
    return Scaffold(
/*
      appBar: AppBar(
        // Here we take the value from the MyHomePage object that was created by
        // the App.build method, and use it to set our appbar title.
        title: Text(widget.title),
      ),
*/
      body: Center(
        widthFactor: 100,
        // Center is a layout widget. It takes a single child and positions it
        // in the middle of the parent.
        child: Column(
          // Column is also a layout widget. It takes a list of children and
          // arranges them vertically. By default, it sizes itself to fit its
          // children horizontally, and tries to be as tall as its parent.
          //
          // Invoke "debug painting" (press "p" in the console, choose the
          // "Toggle Debug Paint" action from the Flutter Inspector in Android
          // Studio, or the "Toggle Debug Paint" command in Visual Studio Code)
          // to see the wireframe for each widget.
          //
          // Column has various properties to control how it sizes itself and
          // how it positions its children. Here we use mainAxisAlignment to
          // center the children vertically; the main axis here is the vertical
          // axis because Columns are vertical (the cross axis would be
          // horizontal).
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Padding(
              padding: EdgeInsets.all(100.0),
              child: Image.asset(
                'images/logo.png',
                width: 200,
                height: 200,
              ),
            ),
            Padding(
                padding: EdgeInsets.all(100.0),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: widgets,
                  ),
                ))
          ],
        ),
      ),
    );
  }
}
