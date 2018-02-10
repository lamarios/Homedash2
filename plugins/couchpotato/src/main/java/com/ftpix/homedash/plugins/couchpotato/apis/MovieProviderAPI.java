package com.ftpix.homedash.plugins.couchpotato.apis;

import com.ftpix.homedash.plugins.couchpotato.models.*;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public abstract class MovieProviderAPI {


    public MovieProviderAPI(String baseUrl, String apiKey, ImagePath imagePath) {
    }

    /**
     * Gets a ramdom poster path to display on the background of the module
     * @return the relative URL to the file
     * @throws Exception
     */
    public abstract String getRandomWantedPoster() throws Exception;

    /**
     * Validates the settings of the instance.
     * @return an empty map or null if everything is fine, a Map<String, String> with error titles and descriptions in case
     */
    public abstract Map<String, String> validateSettings();

    /**
     * Add a movie to the wanted list
     * @param request
     * @throws UnsupportedEncodingException
     * @throws UnirestException
     */
    public abstract void addMovie(MovieRequest request) throws UnsupportedEncodingException, UnirestException;

    /**
     * Search for a movie
     * @param query
     * @return a list of movies
     * @throws Exception
     */
    public abstract List<MovieObject> searchMovie(String query) throws Exception;

    /**
     * Get the base url of the installation
     * @return
     */
    public abstract String getBaseUrl();


    /**
     * List down all the qualities available on the server
     * @return
     * @throws Exception
     */
    public abstract List<QualityProfile> getQualityProfiles() throws Exception;


    /**
     * List down all the possible root folders for the movies
     * @return
     * @throws Exception
     */
    public abstract List<MoviesRootFolder> getMoviesRootFolder() throws Exception;


    /**
     * Gets the name of the API
     * @return
     */
    public abstract String getName();
}
