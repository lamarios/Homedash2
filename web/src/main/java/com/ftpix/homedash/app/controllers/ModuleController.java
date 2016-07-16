package com.ftpix.homedash.app.controllers;


import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ftpix.homedash.app.PluginModuleMaintainer;
import com.ftpix.homedash.models.*;

import com.google.gson.Gson;

import io.gsonfire.GsonFireBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.plugins.Plugin;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import org.eclipse.jetty.http.HttpStatus;

import spark.ModelAndView;
import spark.Spark;
import spark.template.jade.JadeTemplateEngine;

import static com.ftpix.homedash.db.DB.*;

public class ModuleController implements Controller<Module, Integer> {

    private Logger logger = LogManager.getLogger();

    private final Gson gson = new GsonFireBuilder().enableExposeMethodResult().createGsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    public static final String SESSION_NEW_MODULE_PAGE = "new-module-page";


    ///Singleton
    private static ModuleController controller;

    private ModuleController() {
    }

    public static ModuleController getInstance() {
        if (controller == null) {
            controller = new ModuleController();
        }
        return controller;
    }
    // end of singleton

    public void defineEndpoints() {

        /*
         * Add module
		 */
        Spark.get("/add-module/on-page/:page", (req, res) -> {
            logger.info("/add-module");
            try {
                int pageId = Integer.parseInt(req.params("page"));
                Page p = DB.PAGE_DAO.queryForId(pageId);
                if (p != null) {
                    req.session().attribute(SESSION_NEW_MODULE_PAGE, p.getId());

                    Map<String, Object> map = new HashMap<>();
                    map.put("plugins", PluginController.getInstance().listAvailablePlugins());

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
        Spark.get("/add-module/:pluginclass", (req, res) -> {
            Plugin plugin = (Plugin) Class.forName(req.params("pluginclass")).newInstance();

            logger.info("/add-module/{}", plugin.getClass().getCanonicalName());
            try {
                Map<String, Object> map = new HashMap<>();
                map.put("settings", PluginController.getInstance().getPluginSettingsHtml(plugin));
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

                Map<String, String[]> params = req.queryMap().toMap();
                params.put("class", new String[]{plugin.getClass().getCanonicalName()});
                saveModuleWithSettings(params, page);
                res.redirect("/");
                return null;
            }

        }, new JadeTemplateEngine());

		/*
         * Add module
		 */
        Spark.get("/module/:moduleId/settings", (req, res) -> {
            int moduleId = Integer.parseInt(req.params("moduleId"));
            logger.info("/add-module/{}/settings");
            try {
                Plugin plugin = PluginModuleMaintainer.getInstance().getPluginForModule(moduleId);

                Map<String, Object> map = new HashMap<>();
                map.put("plugin", plugin);
                map.put("pluginName", plugin.getDisplayName());
                map.put("settings", PluginController.getInstance().getPluginSettingsHtml(plugin));
                return new ModelAndView(map, "module-settings");
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, new JadeTemplateEngine());

		/*
         * Save a new or edited module
		 */
        Spark.post("/save-module", (req, res) -> {
            logger.info("/save-module");
            try {

                //find page
                int page = 1;
                if (req.session().attribute(SESSION_NEW_MODULE_PAGE) != null) {
                    page = req.session().attribute(SESSION_NEW_MODULE_PAGE);
                }
                logger.info("/save-module ({} params)", req.queryMap().toMap().size());

                //checking settings

                //flattening post query
                Map<String, String> flatSettings = new HashMap<String, String>();
                req.queryMap().toMap().forEach((key, value) -> {
                    flatSettings.put(key, value[0]);
                });

                Plugin plugin;
                boolean editing = false;
                if (flatSettings.containsKey("module_id")) {
                    editing = true;
                    plugin = PluginModuleMaintainer.getInstance().getPluginForModule(Integer.parseInt(flatSettings.get("module_id")));
                } else {
                    plugin = PluginController.getInstance().createPluginFromClass(req.queryParams("class"));
                }


                Map<String, String> errors = plugin.validateSettings(flatSettings);

                //No errors, we're good to go
                if (errors == null || errors.size() == 0) {
                    saveModuleWithSettings(req.queryMap().toMap(), page);
                } else {
                    logger.info("[{}] errors found !", errors.size());
                    Map<String, Object> map = new HashMap<>();
                    if (editing) {
                        map.put("plugin", plugin);
                    } else {
                        map.put("pluginClass", plugin.getClass().getCanonicalName());
                    }
                    map.put("pluginName", plugin.getDisplayName());
                    map.put("settings", PluginController.getInstance().getPluginSettingsHtml(plugin, flatSettings));
                    map.put("errors", errors);
                    return new ModelAndView(map, "module-settings");
                }
            } catch (Exception e) {
                logger.error("Error while saving module", e);
            }
            res.redirect("/");
            return null;
        }, new JadeTemplateEngine());

		/*
         * Deletes a module
		 */
        Spark.delete("/module/:moduleId", (req, res) -> {
            int moduleId = Integer.parseInt(req.params("moduleId"));

            logger.info("/delete-module/{}", moduleId);
            try {
                deleteById(moduleId);

//                res.redirect("/");
                return true;
            } catch (Exception e) {
                logger.error("Error while deleting module", e);
                res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
                return false;
            }

        });


        /**
         * Gets the available sizes for a module's plugin
         */
        Spark.get("/module/:id/availableSizes", (req, res) -> {
            int moduleId = Integer.parseInt(req.params("id"));
            logger.info("/module/{}/availableSizes", moduleId);

            return PluginController.getInstance().getPluginSizes(getModulePlugin(moduleId));
        }, gson::toJson);


        Spark.get("/module/:moduleId/move-to-page/:pageId", (req, res) -> {

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


        Spark.get("/module/:moduleId/full-screen", (req, res) -> {
            Map<String, Object> map = new HashMap<String, Object>();

            int id = Integer.parseInt(req.params("moduleId"));

            Plugin plugin = PluginModuleMaintainer.getInstance().getPluginForModule(id);
            map.put("plugin", plugin);
            map.put("html", plugin.getView(ModuleLayout.FULL_SCREEN));

            return new ModelAndView(map, "module-full-screen");
        }, new JadeTemplateEngine());
    }


    @Override
    public Module get(Integer id) throws SQLException {
        return DB.MODULE_DAO.queryForId(id);
    }

    @Override
    public List<Module> getAll() throws SQLException {
        return DB.MODULE_DAO.queryForAll();
    }

    @Override
    public boolean deleteById(Integer id) throws Exception {
        return delete(get(id));
    }

    @Override
    public boolean delete(Module object) throws Exception {
        deleteModuleLayoutAndSettings(object);
        DB.MODULE_DATA_DAO.delete(object.getData());
        return DB.MODULE_DAO.delete(object) == 1;
    }

    @Override
    public boolean update(Module object) throws SQLException {
        return DB.MODULE_DAO.update(object) == 1;
    }

    @Override
    public Integer create(Module object) throws SQLException {
        DB.MODULE_DAO.create(object);
        return object.getId();
    }

    /**
     * Get all the modules for a specific page
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
     */
    public Plugin getModulePlugin(int moduleId) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
        Module module = MODULE_DAO.queryForId(moduleId);

        Plugin plugin = (Plugin) Class.forName(module.getPluginClass()).newInstance();
        return plugin;
    }

    /**
     * Save a module with its settings
     */
    public int saveModuleWithSettings(Map<String, String[]> postParams, int pageId) throws NumberFormatException, SQLException {
        final Module module;
        if (postParams.containsKey("module_id")) {
            logger.info("Editing a module");
            module = MODULE_DAO.queryForId(Integer.parseInt(postParams.get("module_id")[0]));
        } else {
            logger.info("Creating new module");
            module = new Module();
            module.setPluginClass(postParams.get("class")[0]);
            Page page = DB.PAGE_DAO.queryForId(pageId);
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
                e.printStackTrace();
            }
        });

        logger.info("Module saved, id:[{}]", module.getId());
        return module.getId();
    }

    /**
     * Deletes a module
     */
    public boolean deleteModuleLayoutAndSettings(Module module) throws Exception {
        logger.info("deleteModuleLayoutAndSettings({})", module.getId());
        if (module != null) {
            ModuleLayoutController.getInstance().deleteMany(module.getLayouts());
            ModuleSettingsController.getInstance().deleteMany(module.getSettings());

            PluginModuleMaintainer.getInstance().removeModule(module.getId());
            return true;
        } else {
            return false;
        }
    }


    /**
     * Saves a single module data
     */
    public boolean saveModuleData(ModuleData moduleData) throws SQLException {
        getModuleData(moduleData.getModule().getId(),  moduleData.getName()).ifPresent((data)->{
            moduleData.setId(data.getId());
        });
        return DB.MODULE_DATA_DAO.createOrUpdate(moduleData).getNumLinesChanged() == 1;
    }

    /**
     * Deletes a single module data
     */
    public boolean deleteModuleData(ModuleData moduleData) throws SQLException {
        getModuleData(moduleData.getModule().getId(),  moduleData.getName()).ifPresent((data)->{
            moduleData.setId(data.getId());
        });
        return DB.MODULE_DATA_DAO.delete(moduleData) == 1;
    }


    /**
     * Gets  module data by moduleid and name
     */
    public Optional<ModuleData> getModuleData(int moduleId, String name) throws SQLException {
        QueryBuilder<ModuleData, Integer> queryBuilder = DB.MODULE_DATA_DAO.queryBuilder();
        Where<ModuleData, Integer> where = queryBuilder.where();
        where.eq("module_id", moduleId).and().eq("name", name);

        PreparedQuery<ModuleData> preparedQuery = queryBuilder.prepare();


        ModuleData data = DB.MODULE_DATA_DAO.queryForFirst(preparedQuery);
        if (data != null) {
            return Optional.of(data);
        } else {
            return Optional.empty();
        }
    }
}
