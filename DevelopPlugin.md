# Developing a plugin for HomeDash

Developing a module for HomeDash is pretty simple, you just need to follow a specific file and folder structure

## POM file

Example file from the Hard disk plugin:

```xml
<?xml version="1.0"?>
<project
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
        xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.ftpix.homedash.plugins</groupId>
        <artifactId>plugins</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>harddisk</artifactId>
    <name>harddisk</name>

    <url>http://maven.apache.org</url>
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <sigar.version>1.6.4</sigar.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>3.8.1</version>
            <scope>test</scope>
        </dependency>


    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.lesscss</groupId>
                <artifactId>lesscss-maven-plugin</artifactId>
            </plugin>

            <plugin>
                <groupId>net.tqh.plugins</groupId>
                <artifactId>uglifyjs-maven-plugin</artifactId>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-antrun-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

Things to notice:
1. You need to set a parent POM
2. You need to have the build plugins to compile the less files, uglify the javascript and copy the your assets files where they're supposed to be
3. You can add any other library you might need

## Folder structure

it should be as follow:
```
src
    | main
        | java
            | assets
                | files
                | js
                | less
                | templates
            | com.yourpackage.yourplugin.asyou.wish
```

Things to notice:
1. The 4 folders under the assets package have to be there otherwise the build will fail


## The plugin class

To create a plugin, you need to create a new class file that will implement the Plugin class (com.models.plugins.Plugin)

Commented example:

```java
package com.ftpix.homedash.plugins.harddisk;



import com.ftpix.homedash.Utils.ByteUtils;
import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;

import java.io.File;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by gz on 06-Jun-16.
 */
public class HarddiskPlugin extends Plugin {
    private final String SETTING_PATH = "path";


     /**
     * Unique name for the plugin
     * Better if a simple string without any special characters
     *
     * @return
     */
    @Override
    public String getId() {
        return "harddisk";
    }


    /**
     * Nice to read name of your plugin
     *
     * @return
     */
    @Override
    public String getDisplayName() {
        return "Hard Disk";
    }
    
    
    /**
     * Description of what it's doing
     *
     * @return
     */
    @Override
    public String getDescription() {
        return "Help you monitor the space on a mount point";
    }

    /**
     * Provide an external link if available
     * for example if your plugin refers to an external service
     * returning the url of the service here is nice to have
     *
     * @return null if no link, otherwise an http url
     */
    @Override
    public String getExternalLink() {
        return null;
    }

    /**
     * Give chance to a plugin to run some stuff when creating it Settings can
     * be accessed via settings object
     */
    @Override
    protected void init() {

    }

     /**
      * Get the sizes available for this module
      * Each size should have the format "{width}x{height}" ex 2x4 or 1x1
      * If your module handles full screen view getSizes should contain ModuleLayout.FULL_SCREEN
      * @return an
      */
      @Override
    public String[] getSizes() {
        return new String[]{"1x1", "2x1"};
    }

    /**
     * How often (in second) this module should be refreshed in the background ,
     * 0 = never
     *
     * @return
     */
    @Override
    public int getBackgroundRefreshRate() {
        return 0;
    }

    /**
     * How often (in second) this module should be refreshed in the background ,
     * 0 = never
     * 
     * @return
     */
    @Override
    public WebSocketMessage processCommand(String command, String message, Object extra) {
        return null;
    }



    /**
     * Do background task if getBackgroundRefreshRate() > 0
     */
    @Override
    public void doInBackground() {
    }


    /**
     * Get data to send to clients via web socket
     *
     * @param size of the module
     * @return
     * @throws Exception
     */
    @Override
    protected Object refresh(String size) throws Exception {
        File root = new File(settings.get(SETTING_PATH));

        long usedSpace = root.getTotalSpace() - root.getFreeSpace();

        Map<String, String> diskSpace = new Hashtable<String, String>();


        Map<String, String> spaces = new Hashtable<>();
        //String[] space = new String[] { root.getTotalSpace(), true), humanReadableByteCount(root.getFreeSpace(), true), humanReadableByteCount(usedSpace, true) };
        spaces.put("path", root.getAbsolutePath());
        spaces.put("total", Long.toString(root.getTotalSpace()));
        spaces.put("free", Long.toString(root.getFreeSpace()));
        spaces.put("used", Long.toString(usedSpace));
        spaces.put("pretty", ByteUtils.humanReadableByteCount(usedSpace, root.getTotalSpace(), true));

        return spaces;
    }
    
    
    
    /**
     * Get refresh rate for main page display
     *
     * @return
     */
    @Override
    public int getRefreshRate() {
        return ONE_MINUTE*2;
    }

    /**
     * Validates a given set of settings when user adds the plugin
     *
     * @param settings
     * @return
     */
    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> errors = new Hashtable<>();

        if(!new File(settings.get(SETTING_PATH)).exists()){
            errors.put("Path", "This mount point doesn't exist.");
        }

        return errors;
    }

    /**
     * Expose a chunk of selected data on request
     * This is not mandatory but nice to have. it's used when creating things like Pinned Site live tiles for windows
     *
     * @return
     */
    @Override
    public ModuleExposedData exposeData() {
        ModuleExposedData data = new ModuleExposedData();

        File root = new File(settings.get(SETTING_PATH));
        long usedSpace = root.getTotalSpace() - root.getFreeSpace();

        data.addText(root.getAbsolutePath());
        data.addText(ByteUtils.humanReadableByteCount(usedSpace, root.getTotalSpace(), true));
        return data;
    }

    /**
     * Expose a chunk of selected settings on request
     * Used when showing the available modules to a remote instance
     * DO NOT ADD SENSITIVE DATA HERE it's just to give some hints to user
     * @return
     */
    @Override
    public Map<String, String> exposeSettings() {
        Map<String, String> result = new Hashtable<>();
        result.put("Path", settings.get(SETTING_PATH));
        return result;
    }




}
```

Things to know:

1. Your plugin can save data using the function: setData(String name, Object object);
2. You can retrieved previously saved data via: getData(String name, Class classOfData); or get all with getAllData();
3. Remove data using removeData(String name);
4. You can access the settings via the settings variable

## Templates

You will need to create one template per size your module supports. HomeDash is using JADE template engine.

Templates need to be located in the assets.templates package and you need to follow a specific naming convention

### Settings
if your module has settings it needs to be named {plugin-name}-settings.jade
Example for the harddisk module it needs to be harddisk-settings.jade

Code example for settings template (still following the harddisk module)
```jade
.form-group
    label(for="path") Path
    if settings && settings.containsKey("path")
        input.form-control(type="text", id="path", name="path", placeholder="Absolute Path", value='#{settings.get("path")}')
    else
        input.form-control(type="text", id="path", name ="path", placeholder = "Absolute Path")

```

The name of each field will create a new settings accessible via settings.get("field-name") (check the plugin class example in the refresh() method)

### Plugin views

You need to create one template file per size you support. It needs to follow the following:
[plugin-name]-[size].jade
[plugin-name]-full-screen.jade (if your module has a full page view)

You can put pretty much anything you want in there. 
**Do not put element IDs in your template** it can conflict if your user has more than one instance of your plugin

Example from the Hard disk module on size 2x1 (harddisk-2x1.jade)

```jade
.storage-icon
h4.path
p.data
.hdd-container
```

## CSS

HomeDash is using less css. Style your module using the following rule:
```css
.[plugin-name]{

}

//If you want different CSS for specific size
.[plugin-name].size-[size]{ //example .harddisk.size-2x1{}

}
```

Try to make the content of your module not go over the limit of it's size.
Use overflow: hidden if necessary

## Javascript

As the rest of the files, the javascript need to follow a specific name and structure.

Your javascript files should be located in **assets.js** and follow the following structure:

```javascript
function [plugin-name](moduleId) {

    this.moduleId = moduleId;

    this.onConnect = function () {

    };

    this.documentReady = function (size) {

    };
    
    this.root = function(){
        return rootElement(this.moduleId);
    }

    /*
    * The on message functions are called when your module receives a command from the backend
    * or the normal timed refresh (command == 'refresh' in that case)
    * One function per size your modules handls
    */
    this.onMessage_2x1 = function (command, message, extra) {
      
    };
    this.onMessage_1x1 = function (command, message, extra) {
       
    };
    
    //full screen
    this.onMessage_fullScreen = function (command, message, extra) {
     
    };

}
```

Things to know:
1. You can use jQuery (v2.x)
2. If you need to find element in your DOM you can call the function this.root function. Usefull to add listeners to any buttons or put content in DOM element.

Check the harddisk or any other to get some examples of code.


## Native libraries

If your plugin need to use native libraries, put them in the folder [plugin-root]/lib/native

Your plugin needs to be compiled with the WEB module to be able to work with native libraries. You can't just copy/paste the packaged jar in the distribution plugin folder.


## How to use your plugin

Once HomeDash is started, it should automatically detect your plugin and it should be displayed when trying to add a new module
