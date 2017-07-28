package com.ftpix.homedash.plugin.plex.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Player {
    public String title;

    public static Player fromJson(JSONObject object) {
        Player player = new Player();

        try {
            player.title = object.getString("title");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return player;
    }
}
