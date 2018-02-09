package com.ftpix.homedash.plugins.couchpotato.models;

import com.ftpix.homedash.plugins.couchpotato.apis.CouchPotatoApi;
import com.ftpix.homedash.plugins.couchpotato.apis.RadarrApi;

public enum Type {
    COUCHPOTATO((url, api, imagePath) -> new CouchPotatoApi(url, api, imagePath)),
    RADARR((url, api, imagePath) -> new RadarrApi(url, api, imagePath));

    private final MovieProviderBuilder builder;

    Type(MovieProviderBuilder builder) {
        this.builder = builder;
    }

    public MovieProviderBuilder getBuilder() {
        return builder;
    }
}
