package com.ftpix.homedash.plugin.plex.api.impl;

import com.ftpix.homedash.plugin.plex.api.MediaServerApi;
import com.ftpix.homedash.plugin.plex.model.JellyfinSession;
import com.ftpix.homedash.plugin.plex.model.NowPlaying;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JellyFinApi implements MediaServerApi {
    private Logger logger = LogManager.getLogger();


    private final static String NOW_PLAYING_URL = "%sSessions?ActiveWithinSeconds=960&api_key=%s";
    public static final String IMAGE_URL = "%sItems/%s/Images/backdrop";
    public static final String SETTINGS_URL = "url", SETTINGS_API_KEY = "apiKey";

    private String url, apiKey;

    @Override
    public List<NowPlaying> getNowPlaying() throws UnirestException {
        String toCall = String.format(NOW_PLAYING_URL, url, apiKey);

        HttpResponse<String> response = Unirest.get(toCall).asString();

        String body = response.getBody();
        List<JellyfinSession> sessions = new Gson().fromJson(body, new TypeToken<List<JellyfinSession>>() {
        }.getType());

        return sessions.stream()
                .filter(s -> s.nowPlayingItem != null)
                .map(s -> {
                    NowPlaying np = new NowPlaying();

                    if (s.nowPlayingItem.seriesId != null) {
                        np.setImage(String.format(IMAGE_URL, url, s.nowPlayingItem.seriesId));
                        np.setName(s.nowPlayingItem.seriesName + " - " + s.nowPlayingItem.name);
                    } else {
                        np.setName(s.nowPlayingItem.name);
                        np.setImage(String.format(IMAGE_URL, url, s.nowPlayingItem.id));
                    }

                    int progress = (int) ( ((double) s.playState.positionTicks / (double) s.nowPlayingItem.runtimeTicks) * 100);
                    np.setProgress(progress);

                    np.setPlayer(s.userName + " - " + s.client);

                    return np;

                }).collect(Collectors.toList());

    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        url = settings.get(SETTINGS_URL);
        apiKey = settings.get(SETTINGS_API_KEY);

        if (!url.startsWith("http")) {
            url = "http://" + url;
        }

        if (!url.endsWith("/")) {
            url += "/";
        }

        String toCall = String.format(NOW_PLAYING_URL, url, apiKey);

        logger.info("Testing setting with url:[{}]", toCall);

        try {
            getNowPlaying();
            return null;
        } catch (Exception e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("Connection failed", "Couldn't connect to server: " + e.getMessage());
            return errors;
        }
    }

    @Override
    public void readSettings(Map<String, String> settings) {
        url = settings.get(SETTINGS_URL);
        apiKey = settings.get(SETTINGS_API_KEY);

        if (!url.startsWith("http")) {
            url = "http://" + url;
        }

        if (!url.endsWith("/")) {
            url += "/";
        }

    }
}
