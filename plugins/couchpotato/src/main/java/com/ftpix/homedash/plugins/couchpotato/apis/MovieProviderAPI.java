package com.ftpix.homedash.plugins.couchpotato.apis;

import com.ftpix.homedash.plugins.couchpotato.models.*;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public abstract class MovieProviderAPI {


    public MovieProviderAPI(String baseUrl, String apiKey, ImagePath imagePath) {
    }


    public abstract String getRandomWantedPoster() throws Exception;

    public abstract Map<String, String> validateSettings();

    public abstract void addMovie(MovieObject movie) throws UnsupportedEncodingException, UnirestException;

    public abstract List<MovieObject> searchMovie(String query) throws Exception;

    public abstract String getBaseUrl();

    public abstract List<QualityProfile> getQualityProfiles() throws Exception;

    public abstract List<MoviesRootFolder> getMoviesRootFolder() throws Exception;
}
