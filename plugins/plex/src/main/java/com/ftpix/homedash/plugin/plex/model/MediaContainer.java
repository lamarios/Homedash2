package com.ftpix.homedash.plugin.plex.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MediaContainer {
    public int size;
    public List<Video> videos;

    public static MediaContainer fromJson(JSONObject json) {
        MediaContainer container = new MediaContainer();

        try {
            container.size = json.getInt("size");
        } catch (JSONException e) {
            container.size = 0;
        }

        List<Video> videos = new ArrayList<>();

        try {
            JSONArray videoArray = json.getJSONArray("Metadata");
            videoArray.forEach(object -> {
                videos.add(Video.fromJson((JSONObject) object));
            });
        } catch (JSONException e) {
            try {
                //not an array, but a single object
                videos.add(Video.fromJson(json.getJSONObject("Video")));

            } catch (JSONException e2) {
                //doesn't exist

            }
        }

        container.videos = videos;

        return container;

    }

}
