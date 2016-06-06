package com.ftpix.homedash.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gz on 06-Jun-16.
 */
public class ModuleExposedData {
    private List<String> images = new ArrayList<String>();
    private List<String> texts = new ArrayList<String>();
    private String moduleName = "";

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public List<String> getTexts() {
        return texts;
    }

    public void setTexts(List<String> texts) {
        this.texts = texts;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public void addText(String text) {
        texts.add(text);
    }

    public void addImage(String path) {
        images.add(path);
    }
}

