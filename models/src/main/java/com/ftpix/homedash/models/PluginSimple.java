package com.ftpix.homedash.models;

import com.google.gson.annotations.Expose;

public class PluginSimple {
    @Expose
    private String className;
    @Expose
    private String displayName;
    @Expose
    private String description;

    @Expose
    private boolean settings;

    public PluginSimple(String className, String displayName, String description, boolean settings) {
        this.className = className;
        this.displayName = displayName;
        this.description = description;
        this.settings = settings;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSettings() {
        return settings;
    }

    public void setSettings(boolean settings) {
        this.settings = settings;
    }
}
