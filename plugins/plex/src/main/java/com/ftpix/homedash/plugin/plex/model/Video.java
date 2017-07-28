package com.ftpix.homedash.plugin.plex.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Video {

    public String art;
    public String summary;
    public String type;
    public String title;
    public String grandparentTitle;
    public String viewOffset;
    public String parentTitle;
    public String key;
    public String index;
    public String grandparentThumb;
    public String duration;
    public String thumb;
    public String grandparentArt;
    public String parentArt;
    public String parentThumb;
    public String year;
    public Player player;

    public static Video fromJson(JSONObject json) {
        Video video = new Video();

        try {
            video.art = json.getString("art");
        } catch (JSONException e) {
        }


        try {
            video.duration = json.getString("duration");
        } catch (JSONException e) {
        }

        try {
            video.grandparentArt = json.getString("grandparentArt");
        } catch (JSONException e) {
        }


        try {
            video.grandparentTitle = json.getString("grandparentTitle");
        } catch (JSONException e) {
        }

        try {
            video.grandparentThumb = json.getString("grandparentThumb");
        } catch (JSONException e) {
        }

        try {
            video.parentArt = json.getString("parentArt");
        } catch (JSONException e) {
        }

        try {
            video.parentThumb = json.getString("parentThumb");
        } catch (JSONException e) {
        }

        try {
            video.parentTitle = json.getString("parentTitle");
        } catch (JSONException e) {
        }


        try {
            video.key = json.getString("key");
        } catch (JSONException e) {
        }

        try {
            video.summary = json.getString("summary");
        } catch (JSONException e) {
        }

        try {
            video.thumb = json.getString("thumb");
        } catch (JSONException e) {
        }


        try {
            video.type = json.getString("type");
        } catch (JSONException e) {
        }


        try {
            video.title = json.getString("title");
        } catch (JSONException e) {
        }


        try {
            video.viewOffset = json.getString("viewOffset");
        } catch (JSONException e) {
        }


        try {
            video.year = json.getString("year");
        } catch (JSONException e) {
        }

        try {
            JSONObject object = json.getJSONObject("Player");
            video.player = Player.fromJson(object);
        } catch (JSONException e) {
e.printStackTrace();
        }


        return video;
    }


}
