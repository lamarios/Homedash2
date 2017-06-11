package com.ftpix.homedash.app.controllers;


import com.ftpix.homedash.app.PluginModuleMaintainer;
import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.jobs.BackgroundRefresh;
import com.ftpix.homedash.models.*;
import com.ftpix.homedash.plugins.Plugin;
import com.google.gson.Gson;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import io.gsonfire.GsonFireBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.template.jade.JadeTemplateEngine;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ftpix.homedash.db.DB.MODULE_DAO;
import static com.ftpix.homedash.db.DB.MODULE_SETTINGS_DAO;

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
        Spark.get("/add-module/on-page/:page", this::addModuleOnPage, new JadeTemplateEngine());

		/*
         * Add module with class This will save the module if there are no
		 * settings to display, otherwise it'll show the settings
		 */
        Spark.get("/add-module/:pluginclass", this::addModule, new JadeTemplateEngine());

		/*
         * Add module
		 */
        Spark.get("/module/:moduleId/settings", this::getModuleSettings, new JadeTemplateEngine());

		/*
         * Save a new or edited module
		 */
        Spark.post("/save-module", this::saveModule, new JadeTemplateEngine());

		/*
         * Deletes a module
		 */
        Spark.delete("/module/:moduleId", this::deleteModule);


        /**
         * Gets the available sizes for a module's plugin
         */
        Spark.get("/module/:id/availableSizes", this::getModuleSizes, gson::toJson);


        Spark.get("/module/:moduleId/move-to-page/:pageId", this::moveModule, gson::toJson);


        Spark.get("/module/:moduleId/full-screen", this::getFullScreenView, new JadeTemplateEngine());
    }

    /**
     * Gets the view for a full screen plugin
     *
     * @param req A Spark Request
     * @param res A Spark response
     * @return
     * @throws Exception
     */
    private ModelAndView getFullScreenView(Request req, Response res) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        int id = Integer.parseInt(req.params("moduleId"));

        Plugin plugin = PluginModuleMaintainer.getInstance().getPluginForModule(id);
        map.put("plugin", plugin);
        map.put("html", plugin.getView(ModuleLayout.FULL_SCREEN));

        return new ModelAndView(map, "module-full-screen");
    }

    /**
     * Moves a module to a different page
     *
     * @param req A Spark Request
     * @param res A Spark response
     * @return
     * @throws SQLException
     */
    private boolean moveModule(Request req, Response res) throws SQLException {
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
    }

    /**
     * Gets the available sizes for a module.
     *
     * @param req A Spark Request
     * @param res A Spark response
     * @return a list of String with the different sizes
     */
    private String[] getModuleSizes(Request req, Response res) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        int moduleId = Integer.parseInt(req.params("id"));
        logger.info("/module/{}/availableSizes", moduleId);

        return PluginController.getInstance().getPluginSizes(getModulePlugin(moduleId));
    }

    /**
     * deletes a module
     *
     * @param req A Spark Request
     * @param res A Spark response
     * @return a boolean telling if the deletion has been successful.
     */
    private boolean deleteModule(Request req, Response res) {

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
    }

    /**
     * Save of update a module
     *
     * @param req A Spark Request
     * @param res A Spark response
     * @return the template for the module settings if necessary, redirect to index if the module has no settings.
     */
    private ModelAndView saveModule(Request req, Response res) {

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
                ((Map<String, String>) map.get("errors")).forEach((k, v) -> logger.info("error {} ->{}", k, v));
                return new ModelAndView(map, "module-settings");
            }
        } catch (Exception e) {
            logger.error("Error while saving module", e);
        }
        res.redirect("/");
        return null;
    }

    /**
     * Get and displays the settings for a module
     *
     * @param req A Spark Request
     * @param res A Spark response
     * @return Gets the settings of a module, nothing if we can't get it.
     */
    private ModelAndView getModuleSettings(Request req, Response res) {

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
            logger.error("Couldn't get module settings", e);
            return null;
        }
    }

    /**
     * Adds a module
     *
     * @param req A Spark Request
     * @param res A Spark response
     * @return
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws SQLException
     */
    private ModelAndView addModule(Request req, Response res) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException {

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

    }

    /**
     * Adds a module on a specific page
     *
     * @param req A Spark Request
     * @param res A Spark response
     * @return the compiled template for every plugin available.
     */
    private ModelAndView addModuleOnPage(Request req, Response res) {

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

        BackgroundRefresh.resetTimer();

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
        getModuleData(moduleData.getModule().getId(), moduleData.getName()).ifPresent((data) -> {
            moduleData.setId(data.getId());
        });
        return DB.MODULE_DATA_DAO.createOrUpdate(moduleData).getNumLinesChanged() == 1;
    }

    /**
     * Deletes a single module data
     */
    public boolean deleteModuleData(ModuleData moduleData) throws SQLException {
        getModuleData(moduleData.getModule().getId(), moduleData.getName()).ifPresent((data) -> {
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
