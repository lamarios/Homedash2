package com.ftpix.homedash.plugins.couchpotato.models;

import com.ftpix.homedash.plugins.couchpotato.apis.MovieProviderAPI;

@FunctionalInterface
public interface MovieProviderBuilder {
    MovieProviderAPI build(String url, String apiKey, ImagePath imagePath);
}
