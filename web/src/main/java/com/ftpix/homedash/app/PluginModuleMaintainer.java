package com.ftpix.homedash.app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ftpix.homedash.app.controllers.ModuleController;
import com.ftpix.homedash.app.controllers.PluginController;
import com.ftpix.homedash.models.ModuleData;
import com.ftpix.homedash.plugins.PluginListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.plugins.Plugin;

import javassist.NotFoundException;

public class PluginModuleMaintainer implements PluginListener{

    public final Map<Integer, Plugin> PLUGIN_INSTANCES = new HashMap<>();
    private  Logger logger = LogManager.getLogger();

    private static PluginModuleMaintainer pluginModuleMaintainer;

    private PluginModuleMaintainer(){}

    public static PluginModuleMaintainer getInstance(){

        if(pluginModuleMaintainer == null){
            pluginModuleMaintainer = new PluginModuleMaintainer();
        }

        return pluginModuleMaintainer;
    }


    /**
     * Will get the plug in for a specific module id Will create the instance if
     * not present, will reload the module from DB every time to be sure to get
     * the up to date data
     *
     * @param moduleId
     * @return
     * @throws NotFoundException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public Plugin getPluginForModule(int moduleId) throws Exception {
        logger.info("Looking for module  {}", moduleId);
        Module module = ModuleController.getInstance().get(moduleId);
        if (module != null) {
            return getPluginForModule(module);
        } else {
            throw new NotFoundException("The module doesn't exist !");
        }
    }


    /**
     * Gets the plugin for a particular module
     *
     * @param module
     * @return
     * @throws Exception
     */
    public  Plugin getPluginForModule(Module module) throws Exception {
        logger.debug("Module found, checking if the plugin is already instanciated");
        Plugin plugin = null;
        if (PLUGIN_INSTANCES.containsKey(module.getId())) {
            plugin = PLUGIN_INSTANCES.get(module.getId());

        } else {
            logger.info("Instance doesn't exist, recreating it");
            plugin = PluginController.getInstance().createPluginFromClass(module.getPluginClass());
            plugin.setCacheBase(Constants.CACHE_FOLDER);


        }

        plugin.setModule(module);
        plugin.addListener(this);
        PLUGIN_INSTANCES.put(module.getId(), plugin);
        return plugin;
    }

    /**
     * Same as getPluginForModule but will return all as a list
     *
     * @return
     * @throws Exception
     * @throws SQLException
     */
    public  List<Plugin> getAllPluginInstances() throws SQLException, Exception {
        List<Plugin> plugins = new ArrayList<>();
        ModuleController.getInstance().getAll().forEach((module) -> {
            try {
                plugins.add(getPluginForModule(module));
            } catch (Exception e) {
                logger.error("Can't get plugin for module", e);
            }
        });

        return plugins;
    }

    /**
     * Removes a module from the list after it has been deleted
     *
     * @param moduleId
     */
    public  void removeModule(int moduleId) {
        if (PLUGIN_INSTANCES.containsKey(moduleId)) {
            PLUGIN_INSTANCES.remove(moduleId);
        }
    }


    @Override
    public void saveModuleData(ModuleData data) {
        try {
            ModuleController.getInstance().saveModuleData(data);
        }catch(Exception e){
            logger.error("Error while saving data "+data.getName(), e);
        }
    }

    @Override
    public void removeModuleData(ModuleData data) {
        try {
            ModuleController.getInstance().deleteModuleData(data);
        }catch(Exception e){
            logger.error("Error while saving data "+data.getName(), e);
        }
    }
}
