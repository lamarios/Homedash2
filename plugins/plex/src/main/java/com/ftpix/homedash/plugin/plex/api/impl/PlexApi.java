package com.ftpix.homedash.plugin.plex.api.impl;

import com.ftpix.homedash.plugin.plex.PlexResultParser;
import com.ftpix.homedash.plugin.plex.api.MediaServerApi;
import com.ftpix.homedash.plugin.plex.model.NowPlaying;
import com.ftpix.homedash.plugin.plex.model.PlexSession;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class PlexApi implements MediaServerApi {
    private static final String SETTINGS_TOKEN = "token";
    private static final String SETTINGS_URL = "url";
    private static final String PLEX_SESSIONS_URL = "%sstatus/sessions?X-Plex-Token=%s",
            PLEX_ART_URL = "%s%s?X-Plex-Token=%s",
            PLEX_HEADER_ACCEPT = "Accept",
            PLEX_HEADER_ACCEPT_VALUE = "application/json";

    private Logger logger = LogManager.getLogger();
    private String url = "";
    private String token = "";

    @Override
    public List<NowPlaying> getNowPlaying() throws UnirestException {
        String toCall = String.format(PLEX_SESSIONS_URL, url, token);

        GetRequest get = Unirest.get(toCall).header(PLEX_HEADER_ACCEPT, PLEX_HEADER_ACCEPT_VALUE);
        JsonNode response = get.asJson().getBody();
        PlexSession sessions = PlexResultParser.parseJson(response);

        return Optional.ofNullable(sessions.getMediaContainer())
                .map(c -> c.videos)
                .orElse(Collections.emptyList())
                .stream()
                .map(v -> {
                    NowPlaying np = new NowPlaying();
                    String pictureUrl = "";
                    if (v.type.equalsIgnoreCase("episode")) {
                        String name = "";
                        if (v.grandparentTitle != null && !v.grandparentTitle.equalsIgnoreCase("")) {
                            name = v.grandparentTitle + " - ";
                        }
                        if (v.parentTitle != null && !v.parentTitle.equals("")) {
                            name += v.parentTitle + " - ";
                        }
                        name += v.title;

                        if (v.grandparentArt != null && !v.grandparentArt.equals("")) {
                            pictureUrl = v.grandparentArt;
                        }

                        np.setName(name);

                    } else {
                        np.setName(v.title);
                        pictureUrl = v.art;
                    }

                    pictureUrl = String.format(PLEX_ART_URL, url.substring(0, url.length() - 1), pictureUrl, token);

                    np.setImage(pictureUrl);
                    np.setPlayer(v.player.title);
                    np.setProgress((int) ((Double.parseDouble(v.viewOffset) / Double.parseDouble(v.duration)) * 100));
                    return np;
                }).collect(Collectors.toList());
    }

    @Override
    public void readSettings(Map<String, String> settings) {
        url = settings.get(SETTINGS_URL);
        token = settings.get(SETTINGS_TOKEN);

        if (!url.startsWith("http")) {
            url = "http://" + url;
        }

        if (!url.endsWith("/")) {
            url += "/";
        }
    }


    public Map<String, String> validateSettings(Map<String, String> settings) {
        String url = settings.get(SETTINGS_URL);
        String token = settings.get(SETTINGS_TOKEN);

        if (!url.startsWith("http")) {
            url = "http://" + url;
        }

        if (!url.endsWith("/")) {
            url += "/";
        }

        String toCall = String.format(PLEX_SESSIONS_URL, url, token);

        logger.info("Testing setting with url:[{}]", toCall);

        try {
            GetRequest get = Unirest.get(toCall)
                    .header(PLEX_HEADER_ACCEPT, PLEX_HEADER_ACCEPT_VALUE);

            PlexSession sessions = PlexResultParser.parseJson(get.asJson().getBody());
            return null;
        } catch (UnirestException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("Connection failed", "Couldn't connect to server: " + e.getMessage());
            return errors;
        }

    }
}
