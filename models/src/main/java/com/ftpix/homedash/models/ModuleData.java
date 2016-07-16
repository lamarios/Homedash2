package com.ftpix.homedash.models;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.types.LongStringType;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "module_data")
public class ModuleData {
    @DatabaseField(generatedId = true, allowGeneratedIdInsert = true)
    private int id;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, maxForeignAutoRefreshLevel = 1, uniqueCombo = true)
    private Module module;


    @DatabaseField(uniqueCombo = true)
    private String name;

    @DatabaseField(dataType = DataType.LONG_STRING)
    private String json;


    @DatabaseField
    private String dataClass;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getDataClass() {
        return dataClass;
    }

    public void setDataClass(String dataClass) {
        this.dataClass = dataClass;
    }
}
