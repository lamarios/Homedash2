package com.ftpix.homedash.utils;

import com.ftpix.homedash.app.Constants;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.template.FileTemplateLoader;
import de.neuland.jade4j.template.JadeTemplate;
import spark.ModelAndView;
import spark.TemplateEngine;
import spark.template.jade.loader.SparkClasspathTemplateLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Does the same as usual, just that it has a dev mode to read the files directly so we can reload pages without rebuilding the whole thing
 */
public class HomeDashTemplateEngine extends TemplateEngine {
    private JadeConfiguration configuration;

    public HomeDashTemplateEngine() {
        this("templates");
    }

    public HomeDashTemplateEngine(String templateRoot) {
        if (Constants.DEV_MODE) {
            this.configuration = new JadeConfiguration();

            Path resolve = Paths.get(".").resolve("web").resolve("src").resolve("main").resolve("resources").resolve("templates");
            this.configuration.setTemplateLoader(new FileTemplateLoader(resolve.toString() + "/", StandardCharsets.UTF_8.name()));
        } else {
            this.configuration = new JadeConfiguration();
            this.configuration.setTemplateLoader(new SparkClasspathTemplateLoader(templateRoot));
        }

        Map<String, Object> shared = new HashMap<>();
        shared.put("STATIC_CONFIG", Constants.STATIC_CONFIG);
        this.configuration.setSharedVariables(shared);
    }

    public HomeDashTemplateEngine(JadeConfiguration configuration) {
        this.configuration = configuration;
    }

    public JadeConfiguration configuration() {
        return this.configuration;
    }

    public String render(ModelAndView modelAndView) {
        try {
            JadeTemplate template = this.configuration.getTemplate(modelAndView.getViewName());
            return this.configuration.renderTemplate(template, (Map) modelAndView.getModel());
        } catch (IOException var3) {
            throw new IllegalArgumentException(var3);
        }
    }
}
