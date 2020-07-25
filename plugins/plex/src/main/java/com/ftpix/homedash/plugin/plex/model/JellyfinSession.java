package com.ftpix.homedash.plugin.plex.model;

import com.google.gson.annotations.SerializedName;

public class JellyfinSession {
    @SerializedName("UserName")
    public String userName;
    @SerializedName("Client")
    public String client;

    @SerializedName("NowPlayingItem")
    public JellyfinNowPlaying nowPlayingItem;

    @SerializedName("PlayState")
    public JellyfinPlayState playState;
}
