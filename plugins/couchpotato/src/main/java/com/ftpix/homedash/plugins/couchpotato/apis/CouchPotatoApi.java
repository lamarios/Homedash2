package com.ftpix.homedash.plugins.couchpotato.apis;

import com.ftpix.homedash.plugins.couchpotato.models.ImagePath;
import com.ftpix.homedash.plugins.couchpotato.models.MovieObject;
import com.ftpix.homedash.plugins.couchpotato.models.MoviesRootFolder;
import com.ftpix.homedash.plugins.couchpotato.models.QualityProfile;
import com.google.common.io.Files;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imgscalr.Scalr;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.ftpix.homedash.plugins.couchpotato.CouchPotatoPlugin.THUMB_SIZE;

public class CouchPotatoApi extends MovieProviderAPI {

    private Logger logger = LogManager.getLogger();
    private final String API_MOVIE_SEARCH = "/movie.search/?q=";
    private final String API_ADD_MOVIE = "/movie.add/?title=[TITLE]&identifier=[IMDB]";
    private final String API_AVAILABLE = "/app.available";
    private final String API_MOVIE_LIST = "/movie.list/?status=active";
    private final String baseUrl, url, apiKey;
    private final ImagePath imagePath;

    public CouchPotatoApi(String baseUrl, String apiKey, ImagePath imagePath) {
        super(baseUrl, apiKey, imagePath);
        this.baseUrl = baseUrl;
        this.imagePath = imagePath;

        url = baseUrl + "api/" + apiKey;
        this.apiKey = apiKey;
    }

    @Override
    public String getRandomWantedPoster() throws Exception {
        Unirest.get(url + API_AVAILABLE).asString();

        JSONObject movieList = new JSONObject(Unirest.get(url + API_MOVIE_LIST).asString().getBody());
        String poster = null;
        if (movieList.getBoolean("success")) {
            JSONArray movies = movieList.getJSONArray("movies");
            for (int i = 0; i < movies.length() && poster == null; i++) {
                JSONObject movieInfo = movies.getJSONObject(new Random().nextInt(movies.length())).getJSONObject("info");
                JSONArray images = movieInfo.getJSONObject("images").getJSONArray("poster_original");
                if (images.length() != 0) {

                    // poster = images.getString(new
                    // Random().nextInt(images.length()));
                    File f = new File(imagePath.get() + movieInfo.getString("imdb") + ".jpg");
                    if (!f.exists()) {
                        FileUtils.copyURLToFile(new java.net.URL(images.getString(new Random().nextInt(images.length()))), f);
                        BufferedImage image = ImageIO.read(f);
                        BufferedImage resized = Scalr.resize(image, THUMB_SIZE);
                        ImageIO.write(resized, Files.getFileExtension(f.getName()), f);
                    }
                    poster = imagePath.get() + movieInfo.getString("imdb") + ".jpg";

                }
            }
        }

        return poster;
    }

    @Override
    public Map<String, String> validateSettings() {
        Map<String, String> errors = new Hashtable<String, String>();
        String url = baseUrl + API_AVAILABLE;
        try {
            Unirest.get(url).asString().getBody();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            logger.info("Can't access Couchpotato at URL [{}]", url);
            errors.put("Unavailable", "Couch potato is not available at this URL: " + url);
        }

        return errors;
    }

    @Override
    public void addMovie(MovieObject movie) throws UnsupportedEncodingException, UnirestException {
        String queryUrl = url + API_ADD_MOVIE.replace("[TITLE]", URLEncoder.encode(movie.originalTitle, "UTF-8")).replace("[IMDB]", movie.imdbId);
        Unirest.get(queryUrl).asString().getBody();
    }

    @Override
    public List<MovieObject> searchMovie(String query) throws Exception {
        List<MovieObject> result = new ArrayList<MovieObject>();
        String queryUrl = url + API_MOVIE_SEARCH + URLEncoder.encode(query, "UTF-8");

        String response = Unirest.get(queryUrl).asString().getBody();
        logger.info("Search query:[{}] response:{}", queryUrl, response);

        List<Callable<Void>> pictureDownload = new ArrayList<>();

        try {
            JSONObject json = new JSONObject(response);

            JSONArray jsonarray = json.getJSONArray("movies");

            for (int i = 0; i < jsonarray.length(); i++) {

                JSONObject movie = jsonarray.getJSONObject(i);

                MovieObject movieObject = new MovieObject();
                try {
                    movieObject.imdbId = movie.getString("imdb");

                    JSONArray images = movie.getJSONObject("images").getJSONArray("poster_original");
                    if (images.length() != 0) {
                        File f = new File(imagePath.get() + movieObject.imdbId + ".jpg");
                        if (!f.exists()) {

                            pictureDownload.add(() -> {
                                FileUtils.copyURLToFile(new java.net.URL(images.getString(0)), f);

                                BufferedImage image = ImageIO.read(f);
                                BufferedImage resized = Scalr.resize(image, THUMB_SIZE);
                                ImageIO.write(resized, Files.getFileExtension(f.getName()), f);

                                return null;
                            });

                        }
                        movieObject.poster = imagePath.get() + movieObject.imdbId + ".jpg";
                    }
                } catch (Exception e) {
                    logger.error("Error while parsing JSON");
                    //skipping for this item
                    continue;
                }

                try {
                    movieObject.inLibrary = movie.getBoolean("in_library");
                } catch (JSONException e) {
                    movieObject.inLibrary = true;
                }

                movieObject.originalTitle = movie.getString("original_title");
                try {
                    movieObject.wanted = movie.getBoolean("in_wanted");
                } catch (JSONException e) {
                    movieObject.wanted = true;
                }
                movieObject.year = movie.getInt("year");

                result.add(movieObject);

            }
        } catch (Exception e) {
            logger.info("No movies found");
        }


        //downloading thumbnails
        ExecutorService exec = Executors.newFixedThreadPool(result.size());
        try {
            exec.invokeAll(pictureDownload);
        } finally {
            exec.shutdown();
        }
        return result;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public List<QualityProfile> getQualityProfiles() {
        return null;
    }

    @Override
    public List<MoviesRootFolder> getMoviesRootFolder() throws Exception {
        return null;
    }
}
