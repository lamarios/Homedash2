package com.ftpix.homedash.app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.plugins.Plugin;

import javassist.NotFoundException;

public class PluginModuleMaintainer {

	private static final Map<Integer, Plugin> PLUGIN_INSTANCES = new HashMap<>();
	private static Logger logger = LogManager.getLogger();

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
	public static Plugin getPluginForModule(int moduleId) throws Exception {
		Module module = DB.MODULE_DAO.queryForId(moduleId);

		if (module != null) {
			return getPluginForModule(module);
		} else {
			throw new NotFoundException("The module doesn't exist !");
		}
	}
	
	public static Plugin getPluginForModule(Module module) throws Exception{
		logger.debug("Module found, checking if the plugin is already instanciated");
		Plugin plugin = null;
		if(PLUGIN_INSTANCES.containsKey(module.getId())){
			 plugin = PLUGIN_INSTANCES.get(module.getId());
		}else{
			logger.info("Instance doesn't exist, recreating it");
			 plugin = (Plugin) Class.forName(module.getPluginClass()).newInstance();
		}
		
		plugin.setModule(module);
		PLUGIN_INSTANCES.put(module.getId(), plugin);
		return plugin;
	}
	
	/**
	 * Same as getPluginForModule but will return all as a list
	 * @return
	 * @throws Exception 
	 * @throws SQLException 
	 */
	public static List<Plugin> getAllPluginInstances() throws SQLException, Exception{
		List<Plugin> plugins = new ArrayList<>();
		DB.MODULE_DAO.queryForAll().forEach((module)->{
			try {
				plugins.add(getPluginForModule(module));
			} catch (Exception e) {
				logger.error("Can't get plugin for module", e);
			}
		});
		
		return plugins;
	}
}
