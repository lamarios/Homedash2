package com.ftpix.homedash.Utils;

import de.neuland.jade4j.template.ClasspathTemplateLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class HomeDashClassPathTemplateLoader extends ClasspathTemplateLoader {
    private String templateRoot;

    public HomeDashClassPathTemplateLoader() {

    }

    public HomeDashClassPathTemplateLoader(String templateRoot) {
        if (!templateRoot.endsWith(File.separator)) {
            templateRoot = templateRoot + File.separator;
        }

        this.templateRoot = templateRoot;
    }

    @Override
    public Reader getReader(String name) throws IOException {
        if (this.templateRoot != null) {
            name = this.templateRoot + name;
        }
        name = name.replaceAll("\\\\", "/");
        return new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(name), this.getEncoding());
    }
}

