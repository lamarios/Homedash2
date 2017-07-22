package com.ftpix.homedash.app;

import com.ftpix.homedash.app.controllers.*;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.utils.Predicates;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.ModelAndView;
import spark.template.jade.JadeTemplateEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.get;

public class Endpoints {
    private static Logger logger = LogManager.getLogger();


    public static void define() {

        LayoutController.INSTANCE.defineEndpoints();
        ModuleController.INSTANCE.defineEndpoints();
        PageController.INSTANCE.defineEndpoints();
        PluginController.INSTANCE.defineEndpoints();
        SettingsController.INSTANCE.defineEndpoints();
        ModuleLayoutController.INSTANCE.defineEndpoints();
        ModuleSettingsController.INSTANCE.defineEndpoints();
        RemoteController.INSTANCE.defineEndpoints();
        APIController.INSTANCE.defineEndpoints();
        UpdateController.INSTANCE.defineEndpoints();
        KioskController.INSTANCE.defineEndpoints();
        ;

        /*
         * Main Page
		 */
        get("/", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            try {
                List<Plugin> plugins = PluginModuleMaintainer.INSTANCE.getAllPluginInstances();

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


        cacheResources();
        pluginResources();

    }

    /**
     * Endpoints to access resoruces from cache
     */
    private static void cacheResources() {
        // serving file from cache
        get("/cache/*", (request, response) -> {
            File file = new File(Constants.CACHE_FOLDER + request.splat()[0]);
            logger.info("Looking for file [{}]", file.getAbsolutePath());

            if (file.exists()) {
                response.raw().setContentType("application/octet-stream");
                response.raw().setHeader("Content-Disposition", "attachment; filename=" + file.getName());

                FileInputStream in = new FileInputStream(file);

                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    response.raw().getOutputStream().write(buffer, 0, len);
                }


                in.close();
                return response.raw();
            } else {
                response.status(404);
                return "";
            }

        });
    }

    /**
     * Plugin resources
     */
    private static void pluginResources() {
        get("/plugin/:name/*", (req, res) -> {

            String name = req.params("name");
            String path = req.splat()[0];
            String fullPath = "web/" + name + "/" + path;

            logger.info("/plugin/{}/{}", name, path);

            try {
                Path p = Paths.get(fullPath);
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                InputStream is = classLoader.getResourceAsStream(fullPath);


                res.raw().setContentType(Files.probeContentType(p));
                res.raw().setHeader("Content-Disposition", "inline; filename=" + p.getFileName());

                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    res.raw().getOutputStream().write(buffer, 0, len);
                }

                String result = IOUtils.toString(is);
                is.close();
                return result.trim();
            } catch (Exception e) {
                logger.error("Error while getting resource", e);
                res.status(500);
                return "";
            }
        });
    /*
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
        */

    }
}
