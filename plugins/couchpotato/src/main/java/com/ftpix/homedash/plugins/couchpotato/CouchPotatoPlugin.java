package com.ftpix.homedash.plugins.couchpotato;

import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.plugins.couchpotato.models.MovieObject;
import com.mashape.unirest.http.Unirest;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created by gz on 22-Jun-16.
 */
public class CouchPotatoPlugin extends Plugin {
    public static final String URL = "url", API_KEY = "apiKey";
    public String url, apiKey, baseUrl;

    public static final String METHOD_SEARCH_MOVIE = "searchMovie", METHOD_MOVIE_LIST = "movieList", METHOD_ADD_MOVIE = "addMovie";

    private final String API_MOVIE_SEARCH = "/movie.search/?q=";
    private final String API_ADD_MOVIE = "/movie.add/?title=[TITLE]&identifier=[IMDB]";
    private final String API_AVAILABLE = "/app.available";
    private final String API_MOVIE_LIST = "/movie.list/?status=active";
    private final String IMAGE_PATH = "images/";


    @Override
    public String getId() {
        return "couchpotato";
    }

    @Override
    public String getDisplayName() {
        return "CouchPotato";
    }

    @Override
    public String getDescription() {
        return "Add movies to your CouchPotato wanted list";
    }

    @Override
    public String getExternalLink() {
        return baseUrl;
    }

    @Override
    protected void init() {
        logger.info("Initiating Couchpotato plugin.");

        url = settings.get(URL);

        if (!url.endsWith("/")) {
            url += "/";
        }

        if (!url.startsWith("http")) {
            url = "http://" + url;
        }

        baseUrl = url;
        apiKey = settings.get(API_KEY);

        url += "api/" + apiKey;
        logger.info("Couchpotato URL:{}", url);

        File f = new File(getImagePath());
        if (!f.exists()) {
            f.mkdirs();
        }
        f.deleteOnExit();
    }

    @Override
    public String[] getSizes() {
        return new String[]{"1x1", "1x3", "2x1", "2x2", "2x3", "3x2", "3x3",};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return 0;
    }

    @Override
    protected WebSocketMessage processCommand(String command, String message, Object extra) {
        WebSocketMessage response = new WebSocketMessage();
        if (command.equalsIgnoreCase(METHOD_SEARCH_MOVIE)) {
            try {
                response.setMessage(searchMovie(message));
                response.setCommand(METHOD_MOVIE_LIST);
            } catch (Exception e) {
                logger.error("Error while searching movie", e);
                response.setCommand(WebSocketMessage.COMMAND_ERROR);
                response.setMessage("Error while searching movie.");
            }
        } else if (command.equalsIgnoreCase(METHOD_ADD_MOVIE)) {
            try {
                String[] split = message.split("___");
                addMovie(split[1], split[0]);

                response.setCommand(WebSocketMessage.COMMAND_SUCCESS);
                response.setMessage("Movie added successfully !");
            } catch (Exception e) {
                logger.error("Error while searching movie", e);
                response.setCommand(WebSocketMessage.COMMAND_ERROR);
                response.setMessage("Error while Adding movie.");
            }
        }
        return response;
    }

    @Override
    public void doInBackground() {

    }

    @Override
    protected Object refresh(String size) throws Exception {
        try {
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
                        try {
                            File f = new File(getImagePath() + movieInfo.getString("imdb") + ".jpg");
                            if (!f.exists()) {
                                FileUtils.copyURLToFile(new java.net.URL(images.getString(new Random().nextInt(images.length()))), f);
                            }
                            poster = getImagePath() + movieInfo.getString("imdb") + ".jpg";
                        } catch (Exception e) {
                            logger.error("Can't get movie poster:", e);
                        }

                    }
                }
            }

            return poster;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int getRefreshRate() {
        return ONE_MINUTE * 10;
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {

        Map<String, String> errors = new Hashtable<String, String>();
        String url = settings.get(URL) + API_AVAILABLE;
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
    public ModuleExposedData exposeData() {
        ModuleExposedData data = new ModuleExposedData();

        File f = new File(getImagePath());

        FilenameFilter filter = new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.matches("([^\\s]+(\\.(?i)(jpg|png|gif|bmp))$)");
            }
        };

        List<File> filesArray = Arrays.asList(f.listFiles(filter));
        Collections.shuffle(filesArray);
        if (!filesArray.isEmpty()) {
            data.addImage(getImagePath() + filesArray.get(0).getName());
        }

        return data;
    }

    @Override
    public Map<String, String> exposeSettings() {
        return null;
    }

    ////////////
    /// Plugin methds
    ///

    private String getImagePath() {
        return getCacheFolder() + IMAGE_PATH;
    }


    private void addMovie(String imdbId, String movieName) throws Exception {
        String queryUrl = url + API_ADD_MOVIE.replace("[TITLE]", URLEncoder.encode(movieName, "UTF-8")).replace("[IMDB]", imdbId);
        Unirest.get(queryUrl).asString().getBody();
    }


    /**
     * Search a movie from couchpotato instance
     */
    private List<MovieObject> searchMovie(String query) throws Exception {
        List<MovieObject> result = new ArrayList<MovieObject>();
        String queryUrl = url + API_MOVIE_SEARCH + URLEncoder.encode(query, "UTF-8");

        String response = Unirest.get(queryUrl).asString().getBody();
        logger.info("Search query:[{}] response:{}", queryUrl, response);

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
                        File f = new File(getImagePath() + movieObject.imdbId + ".jpg");
                        if (!f.exists()) {
                            FileUtils.copyURLToFile(new java.net.URL(images.getString(0)), f);
                        }
                        movieObject.poster = getImagePath() + movieObject.imdbId + ".jpg";
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
        return result;
    }
}
