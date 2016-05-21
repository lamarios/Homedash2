package com.ftpix.homedash.app.controllers;

import static com.ftpix.homedash.db.DB.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleSettings;
import com.ftpix.homedash.models.Page;
import com.ftpix.homedash.plugins.Plugin;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

public class ModuleController {

	private Logger logger = LogManager.getLogger();

	/**
	 * Get all the modules for a specific page
	 * 
	 * @param page
	 * @return
	 * @throws SQLException 
	 */
	public List<Module> getModulesForPage(int page) throws SQLException {
		logger.info("Getting modules on page [{}]", page);
		QueryBuilder<Module, Integer> queryBuilder = DB.MODULE_DAO.queryBuilder();
		Where<Module, Integer> where = queryBuilder.where();
		where.eq("page_id", page);

		PreparedQuery<Module> preparedQuery = queryBuilder.prepare();
		
		return DB.MODULE_DAO.query(preparedQuery);
	}
	
	public List<Module> getModulesForPage(Page page) throws SQLException {
		return getModulesForPage(page.getId());
	}
	
	
	/**
	 * Get the plugin class for a module
	 * @throws SQLException 
	 */
	public Plugin getModulePlugin(int moduleId) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Module module = MODULE_DAO.queryForId(moduleId);
		
		Plugin plugin = (Plugin) Class.forName(module.getPluginClass()).newInstance();
		return plugin;
	}

	/**
	 * Save a module with its settings
	 * 
	 * @param postParams
	 * @return
	 * @throws NumberFormatException
	 * @throws SQLException
	 */
	public int saveModuleWithSettings(Map<String, String[]> postParams) throws NumberFormatException, SQLException {
		final Module module;
		if (postParams.containsKey("module_id")) {
			logger.info("Editing a module");
			module = MODULE_DAO.queryForId(Integer.parseInt(postParams.get("module_id")[0]));
		} else {
			logger.info("Creating new module");
			module = new Module();
			module.setPluginClass(postParams.get("class")[0]);
			Page page = DB.PAGE_DAO.queryForId(1);
			logger.info("using page #[{}]:{}", page.getId(), page.getName());
			module.setPage(page);
		}

		MODULE_DAO.createOrUpdate(module);

		MODULE_SETTINGS_DAO.delete(module.getSettings());

		logger.info("[{}] params found", postParams.size());
		postParams.forEach((name, value) -> {
			try {
				if (!name.equalsIgnoreCase("class")) {
					logger.info("Adding setting [{}={}]", name, value[0]);
					ModuleSettings ms = new ModuleSettings();
					ms.setModule(module);
					ms.setName(name);
					ms.setValue(value[0]);
					MODULE_SETTINGS_DAO.create(ms);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		logger.info("Module saved, id:[{}]", module.getId());
		return module.getId();
	}
}
