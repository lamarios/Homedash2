package com.ftpix.homedash.app.controllers;

import com.ftpix.homedash.app.PluginModuleMaintainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.ResponseTransformer;
import spark.Spark;

import java.util.Optional;

public enum PluginUrlController {
    INSTANCE;
    private static final String PATH_PREFIX = "/external/%d%s";
    private final Logger logger = LogManager.getLogger();

    public void defineEndPoints() {
        try {
            PluginModuleMaintainer.INSTANCE.getAllPluginInstances().stream()
                    .filter(p -> Optional.ofNullable(p.defineExternalEndPoints()).isPresent())
                    .filter(p -> !p.defineExternalEndPoints().isEmpty())
                    .forEach(p -> {

                        p.defineExternalEndPoints()
                                .stream()
                                .filter(e -> e.getMethod() != null && e.getRoute() != null && e.getUrl() != null && e.getUrl().startsWith("/"))
                                .forEach(e -> {
                                    String url = String.format(PATH_PREFIX, p.getModule().getId(), e.getUrl());
                                    Optional<ResponseTransformer> transformer = Optional.ofNullable(e.getTransformer());
                                    Optional<String> acceptType = Optional.ofNullable(e.getAcceptType());

                                    logger.info("Defining url [{} {}] for plugin", e.getMethod(), url);
                                    switch (e.getMethod()) {
                                        case GET:
                                            if (transformer.isPresent() && acceptType.isPresent()) {
                                                Spark.get(url, acceptType.get(), e.getRoute(), transformer.get());
                                            } else if (transformer.isPresent() && !acceptType.isPresent()) {
                                                Spark.get(url, e.getRoute(), transformer.get());
                                            } else if (!transformer.isPresent() && acceptType.isPresent()) {
                                                Spark.get(url, acceptType.get(), e.getRoute());
                                            } else {
                                                Spark.get(url, e.getRoute());
                                            }
                                            break;
                                        case POST:
                                            if (transformer.isPresent() && acceptType.isPresent()) {
                                                Spark.post(url, acceptType.get(), e.getRoute(), transformer.get());
                                            } else if (transformer.isPresent() && !acceptType.isPresent()) {
                                                Spark.post(url, e.getRoute(), transformer.get());
                                            } else if (!transformer.isPresent() && acceptType.isPresent()) {
                                                Spark.post(url, acceptType.get(), e.getRoute());
                                            } else {
                                                Spark.post(url, e.getRoute());
                                            }
                                            break;
                                        default:

                                    }

                                });

                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
