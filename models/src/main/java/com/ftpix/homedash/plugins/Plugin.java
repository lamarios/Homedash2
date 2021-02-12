package com.ftpix.homedash.plugins;

import com.ftpix.homedash.Utils.HomeDashClassPathTemplateLoader;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.exceptions.JadeException;
import de.neuland.jade4j.template.FileTemplateLoader;
import de.neuland.jade4j.template.JadeTemplate;
import de.neuland.jade4j.template.TemplateLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.channels.NotYetBoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public abstract class Plugin {

    private final static String REMOTE_URL = "url", REMOTE_API_KEY = "key", REMOTE_MODULE_ID = "id";
    private final static boolean DEV_MODE = Boolean.parseBoolean(System.getProperty("dev", "false"));
    public static int NEVER = 0, ONE_SECOND = 1, ONE_MINUTE = 60, ONE_HOUR = 60 * ONE_MINUTE;
    protected Map<String, String> settings;
    protected Gson gson = new GsonBuilder().create();
    private Logger logger = LogManager.getLogger();
    private String cacheBase;
    private Module module;
    private List<PluginListener> listeners = new ArrayList<>();
    private AtomicInteger clients = new AtomicInteger(0);

    public Plugin() {
    }

    public Plugin(Module module) {
        this.module = module;

    }

    /**
     * Unique name for the plugin
     * Better if a simple string without any special characters
     */
    public abstract String getId();

    /**
     * Nice to read name of your plugin
     */
    public abstract String getDisplayName();

    /**
     * Description of what it's doing
     */
    public abstract String getDescription();

    /**
     * Provide an external link if available
     * for example if your plugin refers to an external service
     * returning the url of the service here is nice to have
     *
     * @return null if no link, otherwise an http url
     */
    public abstract String getExternalLink();

    /**
     * Give chance to a plugin to run some stuff when creating it Settings can
     * be accessed via settings object
     */
    protected abstract void init();

    /**
     * Get the sizes available for this module
     * Each size should have the format "{width}x{height}" ex 2x4 or 1x1
     * If your module handles full screen view getSizes should contain ModuleLayout.FULL_SCREEN
     *
     * @return an
     */
    public abstract String[] getSizes();

    /**
     * How often (in second) this module should be refreshed in the background ,
     * 0 = never
     */
    public abstract int getBackgroundRefreshRate();

    /**
     * Process a command sent by a client
     */
    protected abstract WebSocketMessage processCommand(String command, String message, Object extra);

    /**
     * Do background task if getBackgroundRefreshRate() > 0
     */
    public abstract void doInBackground();
    public abstract  boolean hasSettings();

    /**
     * Get data to send to clients via web socket
     *
     * @param size of the module
     */
    protected abstract Object refresh(String size) throws Exception;

    /**
     * Get refresh rate in seconds for main page display
     *
     * @param size size of the module being refreshed
     */
    public abstract int getRefreshRate(String size);

    /**
     * Validates a given set of settings when user adds the plugin
     */
    public abstract Map<String, String> validateSettings(Map<String, String> settings);

    /**
     * Expose a chunk of selected data on request This is not mandatory but nice to have. it's used
     * when creating things like Pinned Site live tiles for windows
     */
    public abstract ModuleExposedData exposeData();

    /**
     * Expose a chunk of selected settings on request
     * Used when showing the available modules to a remote instance
     * DO NOT ADD SENSITIVE DATA HERE it's just to give some hints to user
     */
    public abstract Map<String, String> exposeSettings();


    /**
     * Do something when the first websocket client connects
     */
    protected abstract void onFirstClientConnect();


    /**
     * Do something when the last websocket client disconnects
     */
    protected abstract void onLastClientDisconnect();


    /**
     * Any data that might be useful to the settings screen of the plugin.
     *
     * @param settings
     * @return anything useful, see Dynamic DNS plugin for example
     */
    protected Object getSettingsScreenData(Map<String, String> settings) {
        return null;
    }

    /**
     * Gets the html for a specific size
     */
    public final String getView(String size) throws JadeException, IOException {


        JadeConfiguration config = defaultTemplateConfig();
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("module", module);

        logger().info("id:[{}]", getId());
        logger().info("size:[{}]", size);
        JadeTemplate template = config.getTemplate("templates/" + getId() + "-" + size + ".jade");

        return config.renderTemplate(template, model);
    }


    /**
     * Gets the jadeconfiguration to get templates
     *
     * @return
     * @throws IOException
     */
    private JadeConfiguration defaultTemplateConfig() throws IOException {
        JadeConfiguration config = new JadeConfiguration();

        TemplateLoader loader = getTemplateLoader(null);

        config.setTemplateLoader(loader);

        return config;
    }

    /**
     * Refresh called from the websocket
     */
    public final WebSocketMessage refreshPlugin(String size) throws Exception {
        WebSocketMessage result = new WebSocketMessage();
        result.setCommand(WebSocketMessage.COMMAND_REFRESH);

        try {
            switch (module.getLocation()) {
                case LOCAL:
                    result.setMessage(refresh(size));
                    break;
                case REMOTE:
                    result.setMessage(refreshRemote(size));
                    break;
                default:
                    return null;
            }

        } catch (Exception e) {
            logger().error("Error while refreshing module", e);
            result.setCommand(WebSocketMessage.COMMAND_ERROR);
            result.setMessage("Can't refresh module:" + e.getMessage());
        }
        result.setModuleId(module.getId());

        return result;
    }

    /**
     * Processes an incoming command from the front end
     *
     * @param command the command name
     * @param message the content of the command
     * @param extra   any extra object that the front end might want to add
     * @return a {@link WebSocketMessage} to be sent to the front end
     */
    public final WebSocketMessage processIncomingCommand(String command, String message, Object extra) {
        switch (module.getLocation()) {
            case LOCAL:
                return processCommand(command, message, extra);
            case REMOTE:
                return processCommandRemote(command, message, extra);
            default:
                return null;
        }

    }


    /**
     * Gets the settings view if there's any
     */
    public final String getSettingsHtml() throws Exception {
        if (module != null) {
            return getSettingsHtml(getSettingsAsMap());
        } else {
            return getSettingsHtml(null);
        }
    }

    public final String getSettingsHtml(Map<String, String> settings) throws Exception {
        try {


            logger().info("Getting settings for [{}]", this.getId());
            JadeConfiguration config = defaultTemplateConfig();

            Map<String, Object> model = new HashMap<String, Object>();

            if (settings != null) {
                model.put("settings", settings);
            }


            Optional.ofNullable(getSettingsScreenData(settings))
                    .ifPresent(s -> model.put("data", s));

            Optional.ofNullable(getSettingsModel()).ifPresent(pluginSettingsModel -> model.put("model", pluginSettingsModel));

            String templateFile = "templates/" + getId() + "-settings.jade";
            logger().info("Looking for template: [{}]", templateFile);
            JadeTemplate template = config.getTemplate(templateFile);

            logger().info("Found setting template, returning it");
            return config.renderTemplate(template, model);
        } catch (Exception e) {
            logger().error("Error while getting settings template", e);
            throw e;
        }
    }

    /**
     * GEts the template loader based on the developer mode value
     *
     * @param loader
     * @return
     * @throws IOException
     */
    private final TemplateLoader getTemplateLoader(TemplateLoader loader) throws IOException {
        if (DEV_MODE) {
            //getting template
            Path plugin = Paths.get(".").resolve("plugins").resolve(getId()).toAbsolutePath();

            Optional<Path> optionalPath = Files.walk(plugin)
                    .filter(p -> Files.isDirectory(p) && p.toAbsolutePath().endsWith("assets/"))
                    .findFirst();


            if (optionalPath.isPresent()) {
                String basePath = optionalPath.get().toAbsolutePath().toString() + "/";
                loader = new FileTemplateLoader(basePath, StandardCharsets.UTF_8.name());
            }
        } else {
            loader = new HomeDashClassPathTemplateLoader();
        }
        return loader;
    }

    protected abstract Map<String, Object> getSettingsModel();

    /**
     * Get the module settings as a map
     */
    public final Map<String, String> getSettingsAsMap() {
        Map<String, String> settings = new HashMap<>();

        module.getSettings().forEach(ms -> {
            settings.put(ms.getName(), ms.getValue());
        });

        return settings;
    }

    /**
     * Get the module
     */
    public final Module getModule() {
        return module;
    }

    /**
     * Set the module, it will load the settings and the data from the module as
     * well
     */
    public final void setModule(Module module) {
        Map<String, String> oldSettings = null;
        if (this.module != null) {

            oldSettings = this.settings;
        }
        this.module = module;

        this.settings = getSettingsAsMap();

        if (module.getLocation() == ModuleLocation.LOCAL && (oldSettings == null || !this.settings.equals(oldSettings))) {
            init();
        }

    }

    public final void setCacheBase(String cacheBase) {
        this.cacheBase = cacheBase;
        if (!cacheBase.endsWith("/")) {
            this.cacheBase += "/";
        }
    }

    protected final Path getCacheFolder() throws NotYetBoundException, IOException {
        if (module != null) {

            Path p = Paths.get(cacheBase + module.getId() + "/");

            if (!Files.exists(p)) {
                Files.createDirectories(p);
            }
            return p;
        } else {
            throw new NotYetBoundException();
        }
    }

    /**
     * Gives the path to use for a given path file
     *
     * @param fileCachePath
     * @return
     */
    protected final String getCacheFileUrlPath(String fileCachePath) {
        final String relativeCacheLocation = fileCachePath.replaceAll(cacheBase, "");
        return "/cache" + (relativeCacheLocation.startsWith("/") ? "" : "/") + relativeCacheLocation;
    }

    /**
     * Boolean to check if a plugin has an external link
     */
    public final boolean hasExternalLink() {
        return getExternalLink() != null;
    }

    /**
     * Boolean to check if an array has full screen view
     */
    public final boolean hasFullScreen() {
        return Arrays.stream(getSizes()).anyMatch((s) -> s.equalsIgnoreCase(ModuleLayout.FULL_SCREEN));
    }

    /**
     * Adds a listener to the plugin listeners
     */
    public final void addListener(PluginListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public final void removeListener(PluginListener listener) {
        listeners.remove(listener);
    }

    /**
     * Sets data for a module
     */
    protected final void setData(String name, Object object) {
        logger().info("Saving data for module");
        ModuleData moduleData = new ModuleData();
        moduleData.setModule(module);
        moduleData.setName(name);
        moduleData.setDataClass(object.getClass().getCanonicalName());
        moduleData.setJson(gson.toJson(object));

        listeners.forEach(l -> l.saveModuleData(moduleData));
    }

    /**
     * Get module data
     *
     * @param type expected type
     */
    protected final Optional getData(String name, Type type) {
        try {
            Optional<ModuleData> filtered = module.getData().stream().filter(data -> data.getName().equalsIgnoreCase(name))
                    .findFirst();

            if (filtered.isPresent()) {

                ModuleData data = filtered.get();

                Class clazz = Class.forName(data.getDataClass());

                Object o = gson.fromJson(data.getJson(), type);

                return Optional.of(clazz.cast(o));
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            logger().error("Error while getting the data", e);
            return Optional.empty();
        }
    }

    /**
     * Get all the data as a map
     */
    protected final Map<String, Object> getAllData() {
        Map<String, Object> data = new HashMap<>();

        module.getData().forEach(singleData -> {
            try {
                Class clazz = Class.forName(singleData.getDataClass());

                Object o = gson.fromJson(singleData.getJson(), clazz);
                data.put(singleData.getName(), o);
            } catch (Exception e) {
                logger().error("Error while getting module data [" + singleData.getName() + "]", e);
            }
        });

        return data;
    }

    /**
     * Remove a specific set of data
     */
    protected final void removeData(String name) {
        List<ModuleData> data = module.getData().stream().filter(d -> d.getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());

        if (!data.isEmpty()) {
            listeners.forEach(l -> l.removeModuleData(data.get(0)));
        }
    }


    /**
     * Refresh a remote module
     */
    private final Object refreshRemote(String size) {

        if (module != null && module.getLocation() == ModuleLocation.REMOTE) {
            String url = settings.get(REMOTE_URL) + "api/refresh/" + settings.get(REMOTE_MODULE_ID) + "/size/" + size;
            String apiKey = settings.get(REMOTE_API_KEY);

            try {
                HttpResponse<JsonNode> response = Unirest.get(url).header("Authorization", apiKey).asJson();

                String jsonString = response.getBody().getObject().toString();

                logger().info("Refreshing remote module, calling [{}], responseL [{}]", url, jsonString);


                // replacing all the cache url calling by the remote one
                jsonString = jsonString.replaceAll("cache/", settings.get(REMOTE_URL) + "cache/");


                WebSocketMessage result = gson.fromJson(jsonString, WebSocketMessage.class);


                return result.getMessage();
            } catch (Exception e) {
                logger().error("Couldn't get remote module [" + settings.get(REMOTE_MODULE_ID) + "] from url: [" + url + "]", e);
                return null;
            }
        } else {
            return null;
        }
    }


    /**
     * Sends command to remote module
     */
    private WebSocketMessage processCommandRemote(String command, String message, Object extra) {
        if (module != null && module.getLocation() == ModuleLocation.REMOTE) {
            String url = settings.get(REMOTE_URL) + "api/process-command/" + settings.get(REMOTE_MODULE_ID);
            String apiKey = settings.get(REMOTE_API_KEY);

            try {
                HttpResponse<JsonNode> response = Unirest.post(url)
                        .header("Authorization", apiKey)
                        .field("command", command)
                        .field("message", message)
                        .field("extra", gson.toJson(extra))
                        .asJson();

                String jsonString = response.getBody().getObject().toString();

                logger().info("Refreshing remote module, calling [{}], responseL [{}]", url, jsonString);

                // replacing all the cache url calling by the remote one
                jsonString = jsonString.replaceAll("cache/", settings.get(REMOTE_URL) + "cache/");


                WebSocketMessage result = gson.fromJson(jsonString, WebSocketMessage.class);
                result.setModuleId(module.getId());


                return result;
            } catch (Exception e) {
                logger().error("Couldn't get remote module [" + settings.get(REMOTE_MODULE_ID) + "] from url: [" + url + "]", e);
                WebSocketMessage result = new WebSocketMessage();
                result.setCommand(WebSocketMessage.COMMAND_ERROR);
                result.setMessage("Can't refresh module:" + e.getMessage());
                result.setModuleId(module.getId());
                return result;
            }
        } else {
            return null;
        }
    }


    /**
     * Increase the number of clients, if it's the first one, do something
     */
    public void increaseClients() {
        if (clients.incrementAndGet() == 1) {
            logger().info("[{}] onFirstClientConnect()", getId());
            onFirstClientConnect();
        }

        logger().info("[{}] has now {} clients", getId(), clients.get());
    }

    /**
     * Decrease the number of clients, if it's the last one, do something
     */
    public void decreaseClients() {
        if (clients.decrementAndGet() == 0) {
            logger().info("[{}] onLastClientDisconnect()", getId());
            onLastClientDisconnect();
        }

        logger().info("[{}] has now {} clients", getId(), clients.get());
    }

    protected Logger logger() {
        ThreadContext.put("logFile", getId());
        return logger;
    }


    /**
     * Define endpoints that external parties can access.
     * The url will start by /external/{moduleid}/{your endpoint}
     * Follow sparkjava definition for parameters
     * <p>
     * YOUR URL MUST START WITH /
     *
     * @return
     */
    public List<ExternalEndPointDefinition> defineExternalEndPoints() {
        return null;
    }
}
