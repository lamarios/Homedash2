package com.ftpix.homedash.app.controllers;

import com.ftpix.homedash.app.Constants;
import com.ftpix.homedash.app.PluginModuleMaintainer;
import com.ftpix.homedash.models.ExposedModule;
import com.ftpix.homedash.models.ModuleLocation;
import com.ftpix.homedash.models.Settings;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.digest.DigestUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.sql.SQLException;
import java.util.*;

/**
 * Created by gz on 22-Jun-16.
 */
public enum APIController {
    INSTANCE;

    private Logger logger = LogManager.getLogger();


    public static final String HEADER_AUTHORIZATION = "Authorization";

    private Gson gson = new GsonBuilder().create();



    public void defineEndpoints() {


        //Filtering API calls
        Spark.before("/api/*", this::beforeFilter);

        Spark.get("/generate-api-key", this::generateApiKey);

        Spark.get("/api/browse", "application/json", this::browse, gson::toJson);


        /**
         * Refresh a remote module
         */
        Spark.get("/api/refresh/:moduleId/size/:size", "application/json", this::refreshModule, gson::toJson);


        /**
         * Process a command from a remote module
         */
        Spark.post("api/process-command/:moduleId", "application/json", this::processCommand, gson::toJson);
    }

    /**
     * Checks if the api key provided is correct
     *
     * @param req
     * @param res
     * @throws SQLException
     */
    private void beforeFilter(Request req, Response res) throws SQLException {

        res.header("Access-Control-Allow-Origin", "*");

        logger.info("API request : [{}]", req.pathInfo());

        Settings useRemote = SettingsController.INSTANCE.get(Settings.USE_REMOTE);
        if (useRemote != null && useRemote.getValue().equalsIgnoreCase("1")) {
            String clientKey = req.headers(HEADER_AUTHORIZATION);

            Settings localKey = SettingsController.INSTANCE.get(Settings.REMOTE_API_KEY);
            if (localKey == null) {
                Spark.halt(401);
            } else if (!localKey.getValue().equalsIgnoreCase(clientKey)) {
                Spark.halt(401);
            }
        } else {
            Spark.halt(401);
        }
    }

    /**
     * Generates an API key for the remote API
     *
     * @param req
     * @param res
     * @return
     */
    private String generateApiKey(Request req, Response res) {
        String toHash = Constants.SALT + new Date() + System.currentTimeMillis();
        return DigestUtils.sha256Hex(toHash);
    }

    /**
     * @param req
     * @param resp
     * @return
     * @throws Exception
     */
    private Map<String, Object> browse(Request req, Response resp) throws Exception {

        Map<String, Object> response = new HashMap<String, Object>();

        response.put("name", SettingsController.INSTANCE.get(Settings.REMOTE_NAME).getValue());

        List<ExposedModule> modules = new ArrayList<ExposedModule>();
        PluginModuleMaintainer.INSTANCE.getAllPluginInstances().stream()
                .filter(p -> p.getModule().getLocation() == ModuleLocation.LOCAL)
                .forEach((plugin) -> {

                    ExposedModule module = new ExposedModule();

                    module.setName(plugin.getDisplayName());
                    module.setDescription(plugin.getDescription());
                    module.setId(plugin.getModule().getId());
                    module.setSettings(plugin.exposeSettings());
                    module.setPluginClass(plugin.getClass().getCanonicalName());
                    modules.add(module);

                });

        response.put("modules", modules);

        return response;
    }

    /**
     * Refresh a module
     *
     * @param req
     * @param res
     * @return
     * @throws Exception
     */
    private WebSocketMessage refreshModule(Request req, Response res) throws Exception {

        String size = req.params("size");
        Integer moduleId = Integer.parseInt(req.params("moduleId"));
        Plugin plugin = PluginModuleMaintainer.INSTANCE.getPluginForModule(moduleId);

        return plugin.refreshPlugin(size);
    }

    /**
     * Processes a command from a remote module
     *
     * @param req
     * @param res
     * @return
     * @throws Exception
     */
    private WebSocketMessage processCommand(Request req, Response res) throws Exception {
        Integer moduleId = Integer.parseInt(req.params("moduleId"));
        Plugin plugin = PluginModuleMaintainer.INSTANCE.getPluginForModule(moduleId);

        String command = req.queryParams("command");
        String message = req.queryParams("message");
        //TODO handle extra
        String extra = null;

        return plugin.processIncomingCommand(command, message, extra);
    }

}
