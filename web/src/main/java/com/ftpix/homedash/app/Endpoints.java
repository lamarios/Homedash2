package com.ftpix.homedash.app;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.ftpix.homedash.app.controllers.PageController;
import com.ftpix.homedash.models.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ftpix.homedash.app.controllers.LayoutController;
import com.ftpix.homedash.app.controllers.ModuleController;
import com.ftpix.homedash.app.controllers.PluginController;
import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.utils.Predicates;
import com.google.gson.Gson;

import io.gsonfire.GsonFireBuilder;
import org.eclipse.jetty.http.HttpStatus;
import spark.ModelAndView;
import spark.template.jade.JadeTemplateEngine;

public class Endpoints {
    private static Logger logger = LogManager.getLogger();

    private static PluginController pluginController = new PluginController();
    private static ModuleController moduleController = new ModuleController();
    private static LayoutController layoutController = new LayoutController();
    private static PageController pageController = new PageController();
    private static final String SESSION_NEW_MODULE_PAGE = "new-module-page";

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
        get("/add-module/on-page/:page", (req, res) -> {
            logger.info("/add-module");
            try {
                int pageId = Integer.parseInt(req.params("page"));
                Page p = DB.PAGE_DAO.queryForId(pageId);
                if (p != null) {
                    req.session().attribute(SESSION_NEW_MODULE_PAGE, p.getId());

                    Map<String, Object> map = new HashMap<>();
                    map.put("plugins", new PluginController().listAvailablePlugins());

                    return new ModelAndView(map, "add-module");
                } else {
                    res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    return null;
                }
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

                //find page
                int page = 1;
                if (req.session().attribute(SESSION_NEW_MODULE_PAGE) != null) {
                    page = req.session().attribute(SESSION_NEW_MODULE_PAGE);
                }

                moduleController.saveModuleWithSettings(req.queryMap().toMap(), page);
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

                //find page
                int page = 1;
                if (req.session().attribute(SESSION_NEW_MODULE_PAGE) != null) {
                    page = req.session().attribute(SESSION_NEW_MODULE_PAGE);
                }

                logger.info("/save-module ({} params)", req.queryMap().toMap().size());
                moduleController.saveModuleWithSettings(req.queryMap().toMap(), page);
            } catch (Exception e) {
                logger.error("Error while saving module", e);
            }
            res.redirect("/");
            return null;
        });

		/*
		 * Deletes a module
		 */
        delete("/module/:moduleId", (req, res) -> {
            int moduleId = Integer.parseInt(req.params("moduleId"));

            logger.info("/delete-module/{}", moduleId);
            try {
                moduleController.deleteModule(moduleId);

//                res.redirect("/");
                return true;
            } catch (Exception e) {
                logger.error("Error while deleting module", e);
                res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
                return false;
            }

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

        /**
         * Settings page
         */
        get("/settings", (req, res) -> {
            List<Settings> settings = DB.SETTINGS_DAO.queryForAll();
            Map<String, String> map = new HashMap<>();

            settings.forEach((setting) -> {
                map.put(setting.getName(), setting.getValue());
            });

            Map<String, Object> model = new HashMap<>();
            model.put("settings", map);

            return new ModelAndView(model, "settings");
        }, new JadeTemplateEngine());

        post("/settings", (req, res) -> {
            Map<String, String[]> postParam = req.queryMap().toMap();

            // checking on checkboxes, if they're not in the params, we need to
            // delete them
            if (!postParam.containsKey(Settings.USE_AUTH)) {
                DB.SETTINGS_DAO.deleteById(Settings.USE_AUTH);
            }

            if (!postParam.containsKey(Settings.PUSHBULLET)) {
                DB.SETTINGS_DAO.deleteById(Settings.PUSHBULLET);
            }

            if (!postParam.containsKey(Settings.PUSHALOT)) {
                DB.SETTINGS_DAO.deleteById(Settings.PUSHALOT);
            }

            if (!postParam.containsKey(Settings.PUSHOVER)) {
                DB.SETTINGS_DAO.deleteById(Settings.PUSHOVER);
            }

            postParam.forEach((name, value) -> {

                Settings setting = new Settings();
                setting.setName(name);

                if (name.equalsIgnoreCase(Settings.PASSWORD)) {
                    // do some encryption
                } else {
                    setting.setValue(value[0]);
                }
                try {
                    DB.SETTINGS_DAO.createOrUpdate(setting);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });

            res.redirect("/settings");
            return "";
        });


        /**
         * gets all pages as json
         */
        get("/pages", (req, res) -> {
            return DB.PAGE_DAO.queryForAll();
        }, gson::toJson);


        /**
         * add a pages and return the whole list of pages as json
         */
        post("/pages/add", (req, res) -> {
            String pageName = req.queryMap("name").value();
            Page page = new Page();
            page.setName(pageName);

            DB.PAGE_DAO.create(page);
            return DB.PAGE_DAO.queryForAll();
        }, gson::toJson);


        /**
         * Rename a page
         */
        post("/pages/edit/:id", (req, res) -> {

            int id = Integer.parseInt(req.params("id"));

            String pageName = req.queryMap("name").value();
            Page page = DB.PAGE_DAO.queryForId(id);

            if (page != null) {
                page.setName(pageName);

                DB.PAGE_DAO.update(page);
                return DB.PAGE_DAO.queryForAll();
            } else {
                res.status(503);
            }

            return "";

        }, gson::toJson);


        /**
         * gets all pages as json
         */
        delete("/page/:id", (req, res) -> {
            int id = Integer.parseInt(req.params("id"));

            if(id > 1) {
                //Deleting all the modules on this page
                pageController.deletePage(id);
            }
            return DB.PAGE_DAO.queryForAll();
        }, gson::toJson);


        get("/module/:moduleId/move-to-page/:pageId", (req, res) -> {

            int moduleId = Integer.parseInt(req.params("moduleId"));
            int pageId = Integer.parseInt(req.params("pageId"));

            logger.info("get /module/{}/move-to-page/{}", moduleId, pageId);

            Module module = DB.MODULE_DAO.queryForId(moduleId);
            Page page = DB.PAGE_DAO.queryForId(pageId);

            if (page != null && module != null) {
                module.setPage(page);
                DB.MODULE_DAO.update(module);
                return true;
            } else {
                return false;
            }

        }, gson::toJson);

        /**
         * Displays layout settings
         */
        get("/layout-settings", (req, res) -> {
            Map<String, Object> model = new HashMap<String, Object>();

            model.put("layouts", DB.LAYOUT_DAO.queryForAll());

            return new ModelAndView(model, "layout-settings");

        }, new JadeTemplateEngine());

        /**
         * Adds a new layout
         */
        post("/layout-settings", (req, res) -> {

            Layout layout = new Layout();
            layout.setName(req.queryParams("name"));
            layout.setMaxGridWidth(1);
            DB.LAYOUT_DAO.create(layout);
            return true;
        });

        /**
         * save layout new size
         */

        get("/layout/:layoutId/set-size/:size", (req, res) -> {
            int layoutId = Integer.parseInt(req.params("layoutId"));
            int size = Integer.parseInt(req.params("size"));

            Layout layout = DB.LAYOUT_DAO.queryForId(layoutId);

            if(layout != null){
                layout.setMaxGridWidth(size);
                DB.LAYOUT_DAO.update(layout);
                return true;
            }else{
                res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
                return false;
            }

        });


    }


    /**
     * Plugin resources
     */
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
