package com.ftpix.homedash.app;

import static spark.Spark.get;
import static spark.Spark.post;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ftpix.homedash.app.controllers.LayoutController;
import com.ftpix.homedash.app.controllers.ModuleController;
import com.ftpix.homedash.app.controllers.PluginController;
import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.utils.Predicates;
import com.google.gson.Gson;

import io.gsonfire.GsonFireBuilder;
import spark.ModelAndView;
import spark.template.jade.JadeTemplateEngine;

public class Endpoints {
	private static Logger logger = LogManager.getLogger();

	private static PluginController pluginController = new PluginController();
	private static ModuleController moduleController = new ModuleController();
	private static LayoutController layoutController = new LayoutController();

	public static void define() {
		final Gson gson = new GsonFireBuilder().enableExposeMethodResult().createGsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

		/*
		 * Main Page
		 */
		get("/", (req, res) -> {
			Map<String, Object> model = new HashMap<>();

			try {
				List<Plugin> plugins = PluginModuleMaintainer.getAllPluginInstances();

				// we need to find all the cs and js files to load
				logger.info("Finding all distinct plugins to load distinct JS files and CSS");
				Object[] filteredPlugins = plugins.stream().filter(Predicates.distinctByKey(p -> p.getId())).toArray();

				logger.info("{} plugins, {} distinct plugins", plugins.size(), filteredPlugins.length);
				model.put("filteredPlugins", filteredPlugins);
				model.put("plugins", plugins);
			} catch (Exception e) {
				e.printStackTrace();

			}
			return new ModelAndView(model, "index");

		}, new JadeTemplateEngine());

		/*
		 * Add module
		 */
		get("/add-module", (req, res) -> {
			logger.info("/add-module");
			try {
				Map<String, Object> map = new HashMap<>();
				map.put("plugins", new PluginController().listAvailablePlugins());

				return new ModelAndView(map, "add-module");
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}, new JadeTemplateEngine());
		
		
		

		/*
		 * Add module with class This will save the module if there are no
		 * settings to display, otherwise it'll show the settings
		 */
		get("/add-module/:pluginclass", (req, res) -> {
			Plugin plugin = (Plugin) Class.forName(req.params("pluginclass")).newInstance();

			logger.info("/add-module/{}", plugin.getClass().getCanonicalName());
			try {
				Map<String, Object> map = new HashMap<>();
				map.put("settings", pluginController.getPluginSettingsHtml(plugin));
				map.put("pluginClass", plugin.getClass().getCanonicalName());
				map.put("pluginName", plugin.getDisplayName());

				return new ModelAndView(map, "module-settings");
			} catch (Exception e) {
				logger.info("no settings to display we save the module");
				moduleController.saveModuleWithSettings(req.queryMap().toMap());
				res.redirect("/");
				return null;
			}

		}, new JadeTemplateEngine());

		/*
		 * Add module
		 */
		get("/module/:moduleId/settings", (req, res) -> {
			int moduleId = Integer.parseInt(req.params("moduleId"));
			logger.info("/add-module/{}/settings");
			try {
				Plugin plugin = PluginModuleMaintainer.getPluginForModule(moduleId);
				
				Map<String, Object> map = new HashMap<>();
				map.put("plugin", plugin);
				map.put("pluginName", plugin.getDisplayName());
				map.put("settings", pluginController.getPluginSettingsHtml(plugin));
				return new ModelAndView(map, "module-settings");
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}, new JadeTemplateEngine());
		
		/*
		 * Save a new or edited module
		 */
		post("/save-module", (req, res) -> {
			logger.info("/save-module");
			try {
				logger.info("/save-module ({} params)", req.queryMap().toMap().size());
				moduleController.saveModuleWithSettings(req.queryMap().toMap());
			} catch (Exception e) {
				logger.error("Error while saving module", e);
			}
			res.redirect("/");
			return null;
		});
		
		
		/*
		 * Deletes a module
		 */
		get("/module/:moduleId/delete", (req, res) -> {
			int moduleId = Integer.parseInt(req.params("moduleId"));

			logger.info("/delete-module/{}", moduleId);
			try {
				DB.MODULE_DAO.deleteById(moduleId);
				PluginModuleMaintainer.removeModule(moduleId);
				
				res.redirect("/");
				return "";
			} catch (Exception e) {
				logger.error("Error while deleting module", e);
			}
			res.redirect("/");
			return null;
		});

		/*
		 * Gets the layout of the modules for the current page
		 */
		get("/modules-layout/:page/:width", (req, res) -> {
			int page = Integer.parseInt(req.params("page"));
			int width = Integer.parseInt(req.params("width"));

			logger.info("/modules-layout/{}/{}", page, width);

			List<ModuleLayout> layouts = layoutController.generatePageLayout(page, width);
			Map<String, Object> model = new HashMap<>();
			model.put("layouts", layouts);
			model.put("plugins", PluginModuleMaintainer.PLUGIN_INSTANCES);
			return new ModelAndView(model, "module-layout");
		}, new JadeTemplateEngine());

		/*
		 * Get HTML of a single module
		 */
		get("/module-content/:moduleId/:size", (req, res) -> {
			int moduleId = Integer.parseInt(req.params("moduleId"));
			String size = req.params("size");

			logger.info("/module-content/{}/{}", moduleId, size);

			return layoutController.getModuleContent(moduleId, size);
		});

		/*
		 * Get the layout based on the screen and send back the info
		 */
		get("/layout-info/json/:width", (req, res) -> layoutController.findClosestLayout(Integer.parseInt(req.params("width"))), gson::toJson);

		/**
		 * save a layout grid
		 */
		post("/save-module-positions/:layoutId", (req, res) -> layoutController.savePositions(Integer.parseInt(req.params("layoutId")), req.queryParams("data")), gson::toJson);

		/**
		 * Gets the available sizes for a module's plugin
		 */
		get("/module/:id/availableSizes", (req, res) -> {
			int moduleId = Integer.parseInt(req.params("id"));
			logger.info("/module/{}/availableSizes", moduleId);

			return pluginController.getPluginSizes(moduleController.getModulePlugin(moduleId));
		}, gson::toJson);

	}

	public static void pluginResources() {
		get("/plugin/:name/files/*", (req, res) -> {

			String name = req.params("name");
			String path = req.splat()[0];
			String fullPath = "web/" + name + "/files/" + path;

			logger.info("/plugin/{}/images/{}", name, path);

			try {
				Path p = Paths.get(fullPath);
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				InputStream is = classLoader.getResourceAsStream(fullPath);

				// res.raw().setContentType("text/javascript");
				res.raw().setHeader("Content-Disposition", "attachment; filename=" + p.getFileName());

				byte[] buffer = new byte[1024];
				int len;
				while ((len = is.read(buffer)) > 0) {
					res.raw().getOutputStream().write(buffer, 0, len);
				}

				is.close();
				return res.raw();
			} catch (Exception e) {
				logger.error("Error while getting resource", e);
				res.status(500);
				return "";
			}
		});

		get("/plugin/:name/js/*", (req, res) -> {

			String name = req.params("name");
			String path = req.splat()[0];
			String fullPath = "web/" + name + "/js/" + path;

			logger.info("/plugin/{}/js/{}", name, path);

			try {
				Path p = Paths.get(fullPath);
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				InputStream is = classLoader.getResourceAsStream(fullPath);

				res.type("text/javascript");
				res.header("Content-Disposition", "attachment; filename=" + p.getFileName());

				String result = IOUtils.toString(is);
				is.close();
				return result;
			} catch (Exception e) {
				logger.error("Error while getting resource", e);
				res.status(500);
				return "";
			}
		});

		get("/plugin/:name/css/*", (req, res) -> {

			String name = req.params("name");
			String path = req.splat()[0];
			String fullPath = "web/" + name + "/css/" + path;

			logger.info("/plugin/{}/css/{}", name, path);

			try {
				Path p = Paths.get(fullPath);
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				InputStream is = classLoader.getResourceAsStream(fullPath);

				res.type("text/css");
				res.header("Content-Disposition", "attachment; filename=" + p.getFileName());

				String result = IOUtils.toString(is);
				is.close();
				return result;
			} catch (Exception e) {
				logger.error("Error while getting resource", e);
				res.status(500);
				return "";
			}
		});
	}
}
