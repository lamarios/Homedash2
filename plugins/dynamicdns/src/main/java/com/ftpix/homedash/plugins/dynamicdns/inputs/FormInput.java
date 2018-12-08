package com.ftpix.homedash.plugins.dynamicdns.inputs;

import com.google.gson.annotations.Expose;

public class FormInput {
    @Expose
    private String name, values, label;
    @Expose
    private FormType type;


    public FormInput(String name, String values, String label, FormType type) {
        super();
        this.name = name;
        this.values = values;
        this.type = type;
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValues() {
        return values;
    }

    public void setValues(String values) {
        this.values = values;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public FormType getType() {
        return type;
    }

    public void setType(FormType type) {
        this.type = type;
    }
}
