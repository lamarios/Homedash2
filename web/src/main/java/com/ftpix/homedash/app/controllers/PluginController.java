package com.ftpix.homedash.app.controllers;

import com.ftpix.homedash.plugins.Plugin;
import de.neuland.jade4j.exceptions.JadeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

import java.io.IOException;
import java.util.*;

public class PluginController {
    private Logger logger = LogManager.getLogger();

    ///Singleton
    private static PluginController controller;

    private PluginController() {
    }

    public static PluginController getInstance() {
        if (controller == null) {
            controller = new PluginController();
        }

        return controller;
    }
    //end of singleton


    public void defineEndpoints() {

    }

    /**
     * Lists all the available controller, looking for class implementing the Plugin class
     *
     * @return
     */
    public List<Plugin> listAvailablePlugins() {
        logger.info("listAvailablePlugins()");
        List<Plugin> availablePlugins = new ArrayList<>();
        Reflections reflections = new Reflections("com.ftpix.homedash");

        Set<Class<? extends Plugin>> subTypes = reflections.getSubTypesOf(Plugin.class);

        logger.info("Found {} plugins", subTypes.size());

        subTypes.forEach(plugin -> {
            try {
                availablePlugins.add(plugin.newInstance());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Error while creating new instance of " + plugin.getCanonicalName(), e);
            }
        });


        Collections.sort(availablePlugins, (o1, o2) -> o1.getDisplayName().compareTo(o2.getDisplayName()));
        return availablePlugins;
    }


    /**
     * @return the html of the plugin
     * @throws IOException
     * @throws JadeException
     */
    public String getPluginSettingsHtml(Plugin plugin, Map<String, String> settings) throws Exception {
        return plugin.getSettingsHtml(settings);
    }

    public String getPluginSettingsHtml(Plugin plugin) throws Exception {
        return plugin.getSettingsHtml();
    }

    /**
     * Get the smallest available size for a plugin
     *
     * @param plugin
     */
    public String getSmallestAvailableSize(Plugin plugin) {
        logger.info("getSmallestAvailableSize [{}]", plugin.getId());
        String[] sizes = plugin.getSizes();
        Arrays.sort(sizes);
        logger.info("Smallest size: {}", sizes[0]);
        return sizes[0];
    }


    public String[] getPluginSizes(Plugin modulePlugin) {
        String[] sizes = modulePlugin.getSizes();
        Arrays.sort(sizes);
        return sizes;
    }

    public Plugin createPluginFromClass(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (Plugin) Class.forName(className).newInstance();
    }
}
