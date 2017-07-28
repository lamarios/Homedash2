package com.ftpix.homedash.models;

import spark.ResponseTransformer;
import spark.Route;
import spark.TemplateViewRoute;

public class ExternalEndPointDefinition {
    public enum Method{
        GET,POST;
    }

    private Method method;
    private String url, acceptType;
    private Route route;
    private ResponseTransformer transformer;

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAcceptType() {
        return acceptType;
    }

    public void setAcceptType(String acceptType) {
        this.acceptType = acceptType;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public ResponseTransformer getTransformer() {
        return transformer;
    }

    public void setTransformer(ResponseTransformer transformer) {
        this.transformer = transformer;
    }
}
