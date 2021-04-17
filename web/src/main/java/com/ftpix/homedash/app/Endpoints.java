package com.ftpix.homedash.app;

import com.ftpix.homedash.app.controllers.*;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.utils.HomeDashTemplateEngine;
import com.ftpix.homedash.utils.Predicates;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lesscss.LessCompiler;
import org.lesscss.LessException;
import spark.ModelAndView;
import spark.Response;
import spark.Route;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        KioskController.INSTANCE.defineEndpoints();
        PluginUrlController.INSTANCE.defineEndPoints();

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

        }, new HomeDashTemplateEngine());


        cacheResources();
        pluginResources();
        staticResources();

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

                try (FileInputStream in = new FileInputStream(file)) {
                    IOUtils.copy(in, response.raw().getOutputStream());
                }
                return "";
            } else {
                response.status(404);
                return "";
            }

        });
    }


    private static void staticResources() {


        Route route = (Route) (req, res) -> {


            if (Constants.DEV_MODE) {
                getDevStaticResource(req.splat()[0], res);
            }

            String fullPath = "web" + req.pathInfo();
            getContent(res, fullPath);

            return "";
        };


        get("/css/*", route);
        get("/fonts/*", route);
        get("/js/*", route);
        get("/images/*", route);
    }


    /**
     * GEts the content of a file from resource with full path
     *
     * @param res
     * @param fullPath
     * @return
     */
    private static void getContent(Response res, String fullPath) {
        try {
            Path p = Paths.get(fullPath);
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            try (InputStream is = classLoader.getResourceAsStream(fullPath)) {


                res.raw().setContentType(Files.probeContentType(p));
                res.raw().setHeader("Content-Disposition", "inline; filename=" + p.getFileName());

                if (p.toString().endsWith(".css")) {
                    res.raw().setHeader("Content-type", " text/css");
                } else if (p.toString().endsWith(".js")) {
                    res.raw().setHeader("Content-type", " application/javascript");
                } else {
                    res.raw().setContentType(Files.probeContentType(p));
                }

                IOUtils.copy(is, res.raw().getOutputStream());

            }
        } catch (Exception e) {
            logger.error("Error while getting resource", e);
            res.status(500);
        }
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

            if (Constants.DEV_MODE) {
                getDevResource(name, path, res);
            } else {
                getContent(res, fullPath);
            }

            return "";
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

    /**
     * GEts dev static resources
     *
     * @param file
     * @return
     */
    private static String getDevStaticResource(String file, Response res) throws IOException, LessException {
        Path assets = Paths.get(".").resolve("web").resolve("src").resolve("main").resolve("java").resolve("assets");

        if (file.endsWith("css")) {
            assets = assets.resolve("less").resolve(file.replaceAll("css", "less"));


            if (Files.exists(assets)) {
                LessCompiler lessCompiler = new LessCompiler();
//                LessCompiler lessCompiler = new LessCompiler(Arrays.asList("--relative-urls", "--strict-math=on"));
                String css = lessCompiler.compile(assets.toFile());

                res.raw().setContentType("text/css");
                res.raw().setHeader("Content-Disposition", "inline; filename=" + assets.getFileName().toString().replaceAll("less", "css"));


                return css;
            } else {
                return null;
            }
        } else if (file.endsWith("js")) {
            assets = assets.resolve("js").resolve(file);

            res.raw().setContentType(Files.probeContentType(assets));
            res.raw().setHeader("Content-Disposition", "inline; filename=" + assets.getFileName());

            if (Files.exists(assets)) {
                return Files.readAllLines(assets).stream().collect(Collectors.joining("\n"));
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Gets the content of file using file system and try to find it in asset folders
     *
     * @param name
     * @return
     */
    private static void getDevResource(String plugin, String name, Response res) throws IOException, LessException {

        Path path = Paths.get(".").toAbsolutePath().resolve("plugins").resolve(plugin);


        if (name.endsWith(".css")) {
            name = name.replaceAll("css", "less");
        }

        String nameFilter = name;

        Optional<Path> optionalPath = Files.walk(path)
                .filter(p -> p.toAbsolutePath().toString().endsWith("assets/" + nameFilter))
                .findFirst();

        if (optionalPath.isPresent()) {


            Path file = optionalPath.get();
            if (name.endsWith("less")) {
                LessCompiler lessCompiler = new LessCompiler();
//                LessCompiler lessCompiler = new LessCompiler(Arrays.asList("--relative-urls", "--strict-math=on"));
                String css = lessCompiler.compile(file.toFile());

                res.raw().setContentType("text/css");
                res.raw().setHeader("Content-Disposition", "inline; filename=" + file.getFileName().toString().replaceAll("less", "css"));


                try (PrintWriter p = new PrintWriter(res.raw().getOutputStream())) {
                    p.println(css);
                }

            } else {
                res.raw().setContentType(Files.probeContentType(file));
                res.raw().setHeader("Content-Disposition", "inline; filename=" + file.getFileName());

                try (FileInputStream fis = new FileInputStream(file.toFile())) {
                    IOUtils.copy(fis, res.raw().getOutputStream());
                }

            }

        }

    }
}
