package com.ftpix.homedash.plugin.plex.api;

import com.ftpix.homedash.plugin.plex.api.impl.JellyFinApi;
import com.ftpix.homedash.plugin.plex.api.impl.PlexApi;
import com.ftpix.homedash.plugin.plex.model.NowPlaying;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.util.List;
import java.util.Map;

public interface MediaServerApi {
    String TYPE = "type";

    static MediaServerApi createFromSettings(Map<String, String> settings) {
        ApiType apiType = ApiType.valueOf(settings.get(TYPE));

        MediaServerApi api = switch (apiType) {
            case PLEX -> new PlexApi();
            case JELLYFIN -> new JellyFinApi();
        };

        api.readSettings(settings);

        return api;
    }

    List<NowPlaying> getNowPlaying() throws UnirestException;

    Map<String, String> validateSettings(Map<String, String> settings);

    void readSettings(Map<String, String> settings);


}
