package com.ftpix.homedash.models;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "modules")
public class Module {

    @DatabaseField(generatedId = true, allowGeneratedIdInsert = true)
    private int id;

    @DatabaseField
    private String pluginClass;

    @DatabaseField(unknownEnumName = "LOCAL")
    private ModuleLocation location = ModuleLocation.LOCAL;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, maxForeignAutoRefreshLevel = 1)
    private Page page;

    @DatabaseField(dataType = DataType.BOOLEAN)
    private boolean onKiosk;

    @ForeignCollectionField(eager = false, maxEagerLevel = 0)
    public ForeignCollection<ModuleSettings> settings;

    @ForeignCollectionField(eager = false, maxEagerLevel = 0)
    public ForeignCollection<ModuleLayout> layouts;

    @ForeignCollectionField(eager = false, maxEagerLevel = 0)
    public ForeignCollection<ModuleData> data;

    public int getId() {
        return id;
    }

    public String getPluginClass() {
        return pluginClass;
    }

    public void setPluginClass(String pluginClass) {
        this.pluginClass = pluginClass;
    }

    public ModuleLocation getLocation() {
        return location;
    }

    public void setLocation(ModuleLocation location) {
        this.location = location;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public ForeignCollection<ModuleSettings> getSettings() {
        return settings;
    }

    public void setSettings(ForeignCollection<ModuleSettings> settings) {
        this.settings = settings;
    }

    public ForeignCollection<ModuleLayout> getLayouts() {
        return layouts;
    }

    public void setLayouts(ForeignCollection<ModuleLayout> layouts) {
        this.layouts = layouts;
    }

    public ForeignCollection<ModuleData> getData() {
        return data;
    }

    public void setData(ForeignCollection<ModuleData> data) {
        this.data = data;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isOnKiosk() {
        return onKiosk;
    }

    public void setOnKiosk(boolean onKiosk) {
        this.onKiosk = onKiosk;
    }
}
