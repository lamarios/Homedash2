package com.ftpix.homedash.plugins.couchpotato.models;

import com.ftpix.homedash.plugins.couchpotato.apis.MovieProviderAPI;

import java.util.function.Function;

@FunctionalInterface
public interface MovieProviderBuilder {
    MovieProviderAPI build(String url, String apiKey, ImagePath imagePath, Function<String, String> cacheToPath);
}
