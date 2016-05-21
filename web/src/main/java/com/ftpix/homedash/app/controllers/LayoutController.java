package com.ftpix.homedash.app.controllers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.Layout;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.Page;
import com.ftpix.homedash.plugins.Plugin;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import de.neuland.jade4j.exceptions.JadeException;

public class LayoutController {
	private Logger logger = LogManager.getLogger();

	/**
	 * Generate a list of module layout based on a page number and window width
	 * 
	 * @param page
	 *            page user is looking at
	 * @param width
	 *            width of his view port
	 * @return
	 * @throws SQLException
	 */
	public List<ModuleLayout> generatePageLayout(int page, int width) throws SQLException {
		Layout closestLayout = findClosestLayout(width);
		Page pageObject = DB.PAGE_DAO.queryForId(page);
		
		return generatePageLayout(pageObject, closestLayout);
	}
	
	public List<ModuleLayout> generatePageLayout(Page page, Layout layout) throws SQLException {
		logger.info("Generating layour for page [{}] and layout[{}]", page.getId(), layout.getName());
		List<ModuleLayout> layouts = new ArrayList<>();

		ModuleController moduleController = new ModuleController();
		List<Module> modules = moduleController.getModulesForPage(page);

		modules.forEach((module) -> {
			try {
				layouts.add(getLayoutForModule(layout, module));
			} catch (Exception e) {
				logger.error("Error while trying to get layout for module", e);
			}
		});

		logger.info("{} modules on page {}", modules.size(), page);

		return layouts;
	}


	/**
	 * This will fine the ModuleLayout for a single module on a single layout If
	 * there isn't it will be created and defaulted to 0x0
	 * 
	 * @param layout
	 * @param module
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public ModuleLayout getLayoutForModule(Layout layout, Module module) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		PluginController pluginController = new PluginController();

		logger.info("Getting Layout for module[{}] on {}", module.getId(), layout.getName());
		QueryBuilder<ModuleLayout, Integer> queryBuilder = DB.MODULE_LAYOUT_DAO.queryBuilder();
		Where<ModuleLayout, Integer> where = queryBuilder.where();
		where.eq("layout_id", layout.getId()).and().eq("module_id", module.getId());

		PreparedQuery<ModuleLayout> preparedQuery = queryBuilder.prepare();

		List<ModuleLayout> moduleLayouts = DB.MODULE_LAYOUT_DAO.query(preparedQuery);
		logger.info("Found {} layouts", moduleLayouts.size());
		ModuleLayout moduleLayout = null;
		if (moduleLayouts.isEmpty()) {
			moduleLayout = new ModuleLayout();
			moduleLayout.setLayout(layout);
			moduleLayout.setModule(module);
			moduleLayout.setX(1);
			moduleLayout.setY(1);

			// Getting the smallest available size for this plugin
			Plugin plugin = (Plugin) Class.forName(module.getPluginClass()).newInstance();
			moduleLayout.setSize(pluginController.getSmallestAvailableSize(plugin));
			DB.MODULE_LAYOUT_DAO.create(moduleLayout);
		} else {
			moduleLayout = moduleLayouts.get(0);
		}

		logger.info("Layout found: moduleId:[{}] x:[{}] y:[{}] size:[{}]", module.getId(), moduleLayout.getX(), moduleLayout.getY(), moduleLayout.getSize());
		return moduleLayout;

	}

	/**
	 * Find the closest layout possible for a window width
	 * 
	 * @param width
	 * @return
	 * @throws SQLException
	 */
	public Layout findClosestLayout(int width) throws SQLException {
		List<Layout> layouts = DB.LAYOUT_DAO.queryForAll();

		// sorting for smallest to biggest.
		Collections.sort(layouts, new Comparator<Layout>() {
			@Override
			public int compare(Layout o1, Layout o2) {
				return Integer.compare(o1.getMaxGridWidth(), o2.getMaxGridWidth());
			}
		});

		Layout selected = null;
		for (Layout layout : layouts) {
			if (layout.getActualSize() <= width) {
				selected = layout;
			} else {
				// we're already bigger, exiting the loop
				break;
			}
		}

		logger.info("Layout size: [{}] window size:[{}]", selected.getActualSize(), width);
		return selected;

	}

	/**
	 * Gets the module HTML
	 * 
	 * @param moduleId
	 * @param size
	 * @return
	 * @throws SQLException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws JadeException
	 * @throws IOException
	 */
	public String getModuleContent(int moduleId, String size) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, JadeException, IOException {

		Module module = DB.MODULE_DAO.queryForId(moduleId);

		Plugin plugin = (Plugin) Class.forName(module.getPluginClass()).newInstance();
		return plugin.getView(size);
	}

	/**
	 * Saves the positions of the items on the grid
	 * 
	 * @param parseInt
	 * @param queryParams
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public boolean savePositions(int layoutId, String queryParams) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		logger.info("savePositions [{}] [{}]", layoutId, queryParams);

		Layout layout = DB.LAYOUT_DAO.queryForId(layoutId);

		String[] split = queryParams.split("-");
		for (String item : split) {
			logger.info("Parsing:[{}]", item);
			String[] itemSplit = item.split(",");
			if (itemSplit.length == 4) {
				int moduleId = Integer.parseInt(itemSplit[0]);
				int x = Integer.parseInt(itemSplit[1]);
				int y = Integer.parseInt(itemSplit[2]);
				String size = itemSplit[3];
				
				Module module = DB.MODULE_DAO.queryForId(moduleId);
				ModuleLayout ml = getLayoutForModule(layout, module);
				ml.setX(x);
				ml.setY(y);
				ml.setSize(size);
				logger.info("Layout update: moduleId:[{}] x:[{}] y:[{}] size:[{}]", module.getId(), ml.getX(), ml.getY(), ml.getSize());

				DB.MODULE_LAYOUT_DAO.update(ml);
			} else {
				logger.error("Wrong String format !");
				return false;
			}
		}
		return true;
	}
}
