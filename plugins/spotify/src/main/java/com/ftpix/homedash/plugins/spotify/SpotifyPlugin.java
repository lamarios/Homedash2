package com.ftpix.homedash.plugins.spotify;

import com.ftpix.homedash.models.ExternalEndPointDefinition;
import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.plugins.spotify.models.Error;
import com.ftpix.homedash.plugins.spotify.models.SpotifyNowPlaying;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.body.MultipartBody;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.util.*;


public class SpotifyPlugin extends Plugin {
    private static final String SETTINGS_CLIENT_SECRET = "client_secret";
    private static final String SETTINGS_CLIENT_ID = "client_id";
    public static final String DATA_SPOTIFY_TOKEN = "spotifyToken";
    public static final String SPOTIFY_API_TOKEN = "https://accounts.spotify.com/api/token";
    public static final String SPOTIFY_CURRENTLY_PLAYING = "http://api.spotify.com/v1/me/player/currently-playing";
    public static final String MESSAGE_TOKEN_EXPIRED = "The access token expired";
    private String clientSecret;
    private String clientID;
    private SpotifyToken token;

    @Override
    public String getId() {
        return "spotify";
    }

    @Override
    public String getDisplayName() {
        return "Spotify";
    }

    @Override
    public String getDescription() {
        return "Show a user's currently playing song";
    }

    @Override
    public String getExternalLink() {
        return null;
    }

    @Override
    protected void init() {
        clientSecret = settings.get(SETTINGS_CLIENT_SECRET);
        clientID = settings.get(SETTINGS_CLIENT_ID);
    }

    @Override
    public String[] getSizes() {
        return new String[]{"1x1", "2x1", "2x2", "3x2", "3x3", "4x4", ModuleLayout.KIOSK};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return 0;
    }

    @Override
    protected WebSocketMessage processCommand(String command, String message, Object extra) {
        return null;
    }

    @Override
    public void doInBackground() {

    }

    @Override
    protected Object refresh(String size) throws Exception {

        getData(DATA_SPOTIFY_TOKEN, SpotifyToken.class).ifPresent(t -> this.token = (SpotifyToken) t);

        try {
            if (token != null) {
                SpotifyNowPlaying nowPlaying = nowPlaying();
                Optional<Error> error = Optional.ofNullable(nowPlaying().error);
                if (!error.isPresent()) {
                    return nowPlaying;
                } else {
                    if (error.get().message.equals(MESSAGE_TOKEN_EXPIRED)) {
                        refreshToken();
                        return refresh(size);
                    } else {
                        return needAuth();
                    }
                }
            } else {
                return needAuth();
            }
        } catch (UnirestException e) {
            return needAuth();
        }


    }

    @Override
    public int getRefreshRate(String size) {
        return ONE_SECOND * 5;
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        return null;
    }

    @Override
    public ModuleExposedData exposeData() {
        return null;
    }

    @Override
    public Map<String, String> exposeSettings() {
        return null;
    }

    @Override
    protected void onFirstClientConnect() {

    }

    @Override
    protected void onLastClientDisconnect() {

    }

    @Override
    protected Map<String, Object> getSettingsModel() {
        return null;
    }


    @Override
    public List<ExternalEndPointDefinition> defineExternalEndPoints() {
        List<ExternalEndPointDefinition> endpoints = new ArrayList<>();

        ExternalEndPointDefinition authorise = new ExternalEndPointDefinition();

        authorise.setMethod(ExternalEndPointDefinition.Method.GET);
        authorise.setUrl("/authorize");
        authorise.setRoute(this::handleAuthorization);

        endpoints.add(authorise);

        return endpoints;
    }


    /**
     * Retunrs to the front end that authorization is required
     *
     * @return
     */
    public Map<String, String> needAuth() {
        logger.info("Couldn't get now playing songs");
        Map<String, String> noAuth = new HashMap<>();
        noAuth.put("clientID", clientID);
        return noAuth;
    }

    /**
     * Gets Spotify currently playing
     *
     * @return
     * @throws UnirestException
     */
    private SpotifyNowPlaying nowPlaying() throws UnirestException {
        String body = Unirest.get(SPOTIFY_CURRENTLY_PLAYING)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.access_token)
                .asString()
                .getBody();

        logger.info("Spotify now playing: {}", body);

        SpotifyNowPlaying spotifyNowPlaying = Optional.ofNullable(gson.fromJson(body, SpotifyNowPlaying.class)).orElse(new SpotifyNowPlaying());

        return spotifyNowPlaying;
    }

    /**
     * Handles incoming authorization requests from Spotify
     *
     * @param req
     * @param res
     * @return
     */
    public String handleAuthorization(Request req, Response res) {
        logger.info("Received authorization request");

        Optional<String> error = Optional.ofNullable(req.queryParams("error"));
        Optional<String> code = Optional.ofNullable(req.queryParams("code")).filter(c -> c.length() > 0);
        Optional<String> state = Optional.ofNullable(req.queryParams("state"));

        if (error.isPresent()) {
            Spark.halt(error.get());
        }

        code.ifPresent(c -> {
            //We have a code we need to get the token
            getToken(c, state.orElse("")).ifPresent(spotifyToken -> {
                token = spotifyToken;
                logger.info("Recieved token with refresh token [{}]", token.refresh_token);
                setData(DATA_SPOTIFY_TOKEN, token);
            });

        });

        return Optional.ofNullable(token).map(t -> "Authorization complete, you can go back to HomeDash").orElse("Authorization failed");

    }


    /**
     * Gets a token after the user has authorized the application
     *
     * @param code the code returned by Spotify
     * @param url  the redirect uri that we used
     * @return
     */
    private Optional<SpotifyToken> getToken(String code, String url) {
        try {
            logger.info("Getting token from code [{}] and state [{}]", code, url);
            MultipartBody post = Unirest.post(SPOTIFY_API_TOKEN).header(HttpHeaders.AUTHORIZATION, "Basic " + getBase64Authorization())
                    .field("grant_type", "authorization_code")
                    .field("code", code)
                    .field("redirect_uri", url);


            String body = post.asString().getBody();
            logger.info("Getting token response with body [{}]", body);

            return Optional.ofNullable(gson.fromJson(body, SpotifyToken.class));
        } catch (UnirestException e) {
            logger.error("Couldn't get Spotify token", e);
            return Optional.empty();
        }
    }


    /**
     * Refreshes a token
     */
    private void refreshToken() {
        Optional.ofNullable(token)
                .map(t -> t.refresh_token)
                .ifPresent(r -> {
                    MultipartBody post = Unirest.post(SPOTIFY_API_TOKEN).header(HttpHeaders.AUTHORIZATION, "Basic " + getBase64Authorization())
                            .field("grant_type", "refresh_token")
                            .field("refresh_token", r);


                    String body = null;
                    try {
                        body = post.asString().getBody();
                        logger.info("Getting token response with body [{}]", body);
                        Optional.ofNullable(gson.fromJson(body, SpotifyToken.class)).ifPresent(t -> {
                            token.access_token = t.access_token;
                            setData(DATA_SPOTIFY_TOKEN, token);

                        });
                    } catch (UnirestException e) {
                        logger.error("couldn't get token via refresh token", e);
                    }

                });
    }

    public String getBase64Authorization() {
        logger.info("Client id [{}], client secret [{}]", clientID, clientSecret);
        return Base64.encodeBase64String((clientID + ":" + clientSecret).getBytes());
    }
}
