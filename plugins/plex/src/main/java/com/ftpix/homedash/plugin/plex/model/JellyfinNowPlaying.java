package com.ftpix.homedash.plugin.plex.model;

import com.google.gson.annotations.SerializedName;

public class JellyfinNowPlaying {
    @SerializedName("Name")
    public String name;
    @SerializedName("Id")
    public String id;
    @SerializedName("SeriesId")
    public String seriesId;
    @SerializedName("SeriesName")
    public String seriesName;

    @SerializedName("RunTimeTicks")
    public long runtimeTicks;
}
