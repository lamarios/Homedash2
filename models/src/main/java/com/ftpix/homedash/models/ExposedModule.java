package com.ftpix.homedash.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gz on 22-Jun-16.
 */
public class ExposedModule {

    private String name;
    private int id;
    private String description;
    private Map<String, String> settings;
    private String pluginClass;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    public String getPluginClass() {
        return pluginClass;
    }

    public void setPluginClass(String pluginClass) {
        this.pluginClass = pluginClass;
    }
}
