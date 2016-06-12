package com.ftpix.homedash.plugins;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.channels.NotYetBoundException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ftpix.homedash.models.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.exceptions.JadeException;
import de.neuland.jade4j.template.ClasspathTemplateLoader;
import de.neuland.jade4j.template.JadeTemplate;
import de.neuland.jade4j.template.TemplateLoader;

public abstract class Plugin {
    protected Logger logger = LogManager.getLogger();

    public static int NEVER = 0, ONE_SECOND = 1, ONE_MINUTE = 60, ONE_HOUR = 60 * ONE_MINUTE;

    private String cacheBase;

    private Module module;

    protected Map<String, String> settings;

    protected Gson gson = new GsonBuilder().create();

    private List<PluginListener> listeners = new ArrayList<>();

    public Plugin() {
    }

    public Plugin(Module module) {
        this.module = module;

    }

    /**
     * Unique name for the plugin
     *
     * @return
     */
    public abstract String getId();

    /**
     * Nice to read name of your plugin
     *
     * @return
     */
    public abstract String getDisplayName();

    /**
     * Description of what it's doing
     *
     * @return
     */
    public abstract String getDescription();


    /**
     * Provide an external link if available
     *
     * @return
     */
    public abstract String getExternalLink();

    /**
     * Give chance to a plugin to run some stuff when creating it
     * Settings can be accessed via settings object
     */
    protected abstract void init();

    /**
     * Get the szes available for this module
     *
     * @return
     */
    public abstract String[] getSizes();

    /**
     * How often (in second) this module should be refreshed in the background ,
     * 0 = never
     *
     * @return
     */
    public abstract int getBackgroundRefreshRate();

    /**
     * Process a command sent by a client
     *
     * @param command
     * @param message
     * @param extra
     * @return
     */
    public abstract WebSocketMessage processCommand(String command, String message, Object extra);

    /**
     * Do background task if any
     */
    public abstract void doInBackground();

    /**
     * Get data to send to clients via web socket
     *
     * @param size of the module
     * @return
     * @throws Exception
     */
    protected abstract Object refresh(String size) throws Exception;

    /**
     * Get refresh rate for front page display
     *
     * @return
     */
    public abstract int getRefreshRate();


    /**
     * Validates a given set of settings
     *
     * @param settings
     * @return
     */
    public abstract Map<String, String> validateSettings(Map<String, String> settings);


    /**
     * Expose a chunk of selected data on request
     *
     * @return
     */
    public abstract ModuleExposedData exposeData();


    /**
     * Expose a chunk of selected settings on request
     *
     * @return
     */
    public abstract Map<String, String> exposeSettings();


    /**
     * Gets the html for a specific size
     *
     * @param size
     * @return
     * @throws JadeException
     * @throws IOException
     */
    public String getView(String size) throws JadeException, IOException {

        JadeConfiguration config = new JadeConfiguration();

        TemplateLoader loader = new ClasspathTemplateLoader();

        config.setTemplateLoader(loader);

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("module", module);

        logger.info("id:[{}]", getId());
        logger.info("size:[{}]", size);
        JadeTemplate template = config.getTemplate("templates/" + getId() + "-" + size + ".jade");

        return config.renderTemplate(template, model);
    }

    /**
     * Refresh called from the websocket
     *
     * @param size
     * @return
     * @throws Exception
     */
    public Object refreshPlugin(String size) throws Exception {
        if (module.getRemote() == module.LOCAL) {
            Object obj = refresh(size);
            return obj;
        } else {
            return null;
        }
    }

    /**
     * Gets the settings view if there's any
     *
     * @return
     * @throws JadeException
     * @throws IOException
     */
    public String getSettingsHtml() throws Exception {
        try {
            logger.info("Getting settings for [{}]", this.getId());
            JadeConfiguration config = new JadeConfiguration();

            TemplateLoader loader = new ClasspathTemplateLoader();

            config.setTemplateLoader(loader);

            Map<String, Object> model = new HashMap<String, Object>();
            if (module != null) {
                model.put("settings", getSettingsAsMap());
            }

            System.out.println(getId());
            String templateFile = "templates/" + getId() + "-settings.jade";
            logger.info("Looking for template: [{}]", templateFile);
            JadeTemplate template = config.getTemplate(templateFile);

            logger.info("Found setting template, returning it");
            return config.renderTemplate(template, model);
        } catch (Exception e) {
            logger.error("Error while getting settings template", e);
            throw e;
        }
    }

    /**
     * Get the module settings as a map
     *
     * @return
     */
    private Map<String, String> getSettingsAsMap() {
        Map<String, String> settings = new HashMap<>();

        module.getSettings().forEach(ms -> {
            settings.put(ms.getName(), ms.getValue());
        });

        return settings;
    }

    /**
     * Get the module
     *
     * @return
     */
    public Module getModule() {
        return module;
    }

    /**
     * Set the module, it will load the settings and the data from the module as
     * well
     *
     * @param module
     */
    public void setModule(Module module) {
        Map<String, String> oldSettings = null;
        if (this.module != null) {

            oldSettings = this.settings;
        }
        this.module = module;

        this.settings = getSettingsAsMap();

        if (oldSettings == null || ! this.settings.equals(oldSettings)) {
            init();
        }


    }

    public void setCacheBase(String cacheBase) {
        this.cacheBase = cacheBase;
        if (!cacheBase.endsWith("/")) {
            this.cacheBase += "/";
        }
    }

    protected String getCacheFolder() throws NotYetBoundException {
        if (module != null) {
            return cacheBase + module.getId() + "/";
        } else {
            throw new NotYetBoundException();
        }
    }

    /**
     * Boolean to check if a plugin has an external link
     *
     * @return
     */
    public boolean hasExternalLink() {
        return getExternalLink() != null;
    }

    /**
     * Boolean to check if an array has full screen view
     *
     * @return
     */
    public boolean hasFullScreen() {
        return Arrays.stream(getSizes()).anyMatch((s) -> s.equalsIgnoreCase(ModuleLayout.FULL_SCREEN));
    }


    /**
     * Adds a listener to the plugin listeners
     *
     * @param listener
     */
    public void addListener(PluginListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }


    public void removeListener(PluginListener listener) {
        listeners.remove(listener);
    }


    /**
     * Sets data for a module
     *
     * @param name
     * @param object
     * @param <T>
     */
    protected <T> void setData(String name, T object) {
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
     * @param name
     * @param classOfT expected class
     * @param <T>
     * @return
     */
    protected <T> T getData(String name, Class<T> classOfT) {
        try {
            List<ModuleData> filtered = module.getData().stream().filter(data -> data.getName().equalsIgnoreCase(name))
                    .collect(Collectors.toList());

            if (!filtered.isEmpty()) {
                T toReturn = null;

                ModuleData data = filtered.get(0);

                Class clazz = Class.forName(data.getDataClass());

                Object o = gson.fromJson(data.getJson(), (Type) classOfT);

                return (T) o;
            }
            return null;
        } catch (Exception e) {
            logger.error("Error while getting the data", e);
            return null;
        }
    }

    /**
     * Get all the data as a map
     *
     * @return
     */
    protected Map<String, Object> getAllData() {
        Map<String, Object> data = new HashMap<>();

        module.getData().forEach(singleData -> {
            try {
                Class clazz = Class.forName(singleData.getDataClass());

                Object o = gson.fromJson(singleData.getJson(), clazz);
                data.put(singleData.getName(), o);
            } catch (Exception e) {
                logger.error("Error while getting module data [" + singleData.getName() + "]", e);
            }
        });

        return data;
    }

    protected void removeData(String name) {
        List<ModuleData> data = module.getData().stream().filter(d -> d.getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());

        if (!data.isEmpty()) {
            listeners.forEach(l -> l.removeModuleData(data.get(0)));
        }

    }
}

