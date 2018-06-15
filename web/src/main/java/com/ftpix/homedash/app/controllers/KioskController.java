package com.ftpix.homedash.app.controllers;

import com.ftpix.homedash.app.PluginModuleMaintainer;
import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.utils.HomeDashTemplateEngine;
import com.ftpix.homedash.utils.Predicates;
import com.google.gson.Gson;
import io.gsonfire.GsonFireBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum KioskController {

    INSTANCE;

    private Logger logger = LogManager.getLogger();

    private final Gson gson = new GsonFireBuilder().enableExposeMethodResult().createGsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    public void defineEndpoints() {
        Spark.get("/kiosk/:moduleId", this::getKioskView, new HomeDashTemplateEngine());
        Spark.get("/kiosk", this::getKioskView, new HomeDashTemplateEngine());
        Spark.get("/kiosk/:moduleId/inRotation", this::isInRotation, gson::toJson);
        Spark.get("/kiosk/:moduleId/toggleRotation", this::toggleRotation, gson::toJson);

    }

    /**
     * Toggle a module's isKiosk variable
     *
     * @param request
     * @param response
     * @return
     */
    private boolean toggleRotation(Request request, Response response) throws SQLException {
        Optional<Module> moduleOptional = Optional.ofNullable(request.params("moduleId"))
                .map(Integer::parseInt)
                .map(i -> {
                    try {
                        return PluginModuleMaintainer.INSTANCE.getPluginForModule(i);
                    } catch (Exception e) {
                        logger.info("Couldn't find module for id {}", i);
                        return null;
                    }
                })
                .filter(p -> Stream.of(p.getSizes()).anyMatch(s -> s.equalsIgnoreCase(ModuleLayout.KIOSK)))
                .map(Plugin::getModule);

        if (moduleOptional.isPresent()) {
            Module module = moduleOptional.get();
            module.setOnKiosk(!module.isOnKiosk());
            DB.MODULE_DAO.update(module);
            return module.isOnKiosk();
        } else {
            return false;
        }
    }

    /**
     * Checks whether a module is in the kiosk rotation
     *
     * @param request
     * @param response
     * @return
     */
    private boolean isInRotation(Request request, Response response) {

        return Optional.ofNullable(request.params("moduleId"))
                .map(Integer::parseInt)
                .map(i -> {
                    try {
                        return PluginModuleMaintainer.INSTANCE.getPluginForModule(i);
                    } catch (Exception e) {
                        logger.info("Couldn't find module for id {}", i);
                        return null;
                    }
                })
                .filter(p -> Stream.of(p.getSizes()).anyMatch(s -> s.equalsIgnoreCase(ModuleLayout.KIOSK)))
                .map(Plugin::getModule)
                .map(Module::isOnKiosk)
                .orElse(false);
    }


    /**
     * Gets the view for a kiosk plugin
     *
     * @param req A Spark Request
     * @param res A Spark response
     * @return
     * @throws Exception
     */
    private ModelAndView getKioskView(Request req, Response res) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        List<Plugin> plugins = Optional.ofNullable(req.params("moduleId"))
                .map(Integer::parseInt)
                .map(i -> {
                    try {
                        return PluginModuleMaintainer.INSTANCE.getPluginForModule(i);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .map(Collections::singletonList)
                .orElse(PluginModuleMaintainer
                        .INSTANCE
                        .getAllPluginInstances()
                        .stream()
                        .filter(p -> Stream.of(p.getSizes()).anyMatch(s -> s.equalsIgnoreCase(ModuleLayout.KIOSK)))
                        .filter(p -> p.getModule().isOnKiosk())
                        .collect(Collectors.toList())
                );

        Object[] filteredPlugins = plugins.stream().filter(Predicates.distinctByKey(p -> p.getId())).toArray();

        //We're only seeing a single module
        map.put("plugins", plugins);
        map.put("filteredPlugins", plugins);
        if (plugins.isEmpty()) {
            Spark.halt("No kiosk plugin available");
        } else {
            map.put("html", plugins.get(0).getView(ModuleLayout.KIOSK));
        }


        return new ModelAndView(map, "module-kiosk");

    }
}
