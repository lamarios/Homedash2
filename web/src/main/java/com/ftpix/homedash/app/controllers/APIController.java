package com.ftpix.homedash.app.controllers;

import com.ftpix.homedash.app.Constants;
import com.ftpix.homedash.app.PluginModuleMaintainer;
import com.ftpix.homedash.models.ExposedModule;
import com.ftpix.homedash.models.ModuleLocation;
import com.ftpix.homedash.models.Settings;
import com.ftpix.homedash.plugins.Plugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.digest.DigestUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Spark;

import java.util.*;

/**
 * Created by gz on 22-Jun-16.
 */
public class APIController {
    private Logger logger = LogManager.getLogger();

    private static final APIController instance = new APIController();

    public static final String HEADER_AUTHORIZATION = "Authorization";

    private Gson gson = new GsonBuilder().create();

    private APIController() {
    }

    public static APIController getInstance() {
        return instance;
    }

    public void defineEndpoints() {


        //Filtering API calls
        Spark.before("/api/*", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");

            logger.info("API request : [{}]", req.pathInfo());

            Settings useRemote = SettingsController.getInstance().get(Settings.USE_REMOTE);
            if (useRemote != null && useRemote.getValue().equalsIgnoreCase("1")) {
                String clientKey = req.headers(HEADER_AUTHORIZATION);

                Settings localKey = SettingsController.getInstance().get(Settings.REMOTE_API_KEY);
                if (localKey == null) {
                    Spark.halt(401);
                } else if (!localKey.getValue().equalsIgnoreCase(clientKey)) {
                    Spark.halt(401);
                }
            } else {
                Spark.halt(401);
            }

        });

        Spark.get("/generate-api-key", (req, res) -> {
            String toHash = Constants.SALT + new Date() + System.currentTimeMillis();
            return DigestUtils.sha256Hex(toHash);
        });

        Spark.get("/api/browse", "application/json", (req, res) -> {
            Map<String, Object> response = new HashMap<String, Object>();

            response.put("name", SettingsController.getInstance().get(Settings.REMOTE_NAME).getValue());

            List<ExposedModule> modules = new ArrayList<ExposedModule>();
            PluginModuleMaintainer.getInstance().getAllPluginInstances().stream()
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

        }, gson::toJson);


        /**
         * Refresh a remote module
         */
        Spark.get("/api/refresh/:moduleId/size/:size", "application/json", (req, res)->{

            String size = req.params("size");
            Integer moduleId = Integer.parseInt(req.params("moduleId"));
            Plugin plugin = PluginModuleMaintainer.getInstance().getPluginForModule(moduleId);

            return  plugin.refreshPlugin(size);

        }, gson::toJson);


        /**
         * Process a command fromna remote module
         */
        Spark.post("api/process-command/:moduleId", "application/json", (req, res)->{

            Integer moduleId = Integer.parseInt(req.params("moduleId"));
            Plugin plugin = PluginModuleMaintainer.getInstance().getPluginForModule(moduleId);

            String command = req.queryParams("command");
            String message= req.queryParams("message");
            //TODO handle extra
            String extra = null;

            return plugin.processIncomingCommand(command, message, extra);
        }, gson::toJson);
    }

}
