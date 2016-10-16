package com.ftpix.homedash.plugins.dynamicdns.inputs;

public class FormInput {
    private String name, values, label;
    private int type;
    public static final int TYPE_TEXT = 0, TYPE_PASSWORD = 1, TYPE_CHECKBOX = 2, TYPE_SELECT = 3;


    public FormInput(String name, String values, String label, int type) {
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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }


}
