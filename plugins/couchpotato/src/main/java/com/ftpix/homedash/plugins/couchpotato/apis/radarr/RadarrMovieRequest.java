package com.ftpix.homedash.plugins.couchpotato.apis.radarr;

import java.util.List;

public class RadarrMovieRequest {
    private String title, titleSlug, rootFolderPath;
    private int tmdbId, qualityProfileId;
    private boolean monitored;
    private List<RadarrImage> images;


    public List<RadarrImage> getImages() {
        return images;
    }

    public void setImages(List<RadarrImage> images) {
        this.images = images;
    }

    public boolean isMonitored() {
        return monitored;
    }

    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleSlug() {
        return titleSlug;
    }

    public void setTitleSlug(String titleSlug) {
        this.titleSlug = titleSlug;
    }

    public String getRootFolderPath() {
        return rootFolderPath;
    }

    public void setRootFolderPath(String rootFolderPath) {
        this.rootFolderPath = rootFolderPath;
    }

    public int getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(int tmdbId) {
        this.tmdbId = tmdbId;
    }

    public int getQualityProfileId() {
        return qualityProfileId;
    }

    public void setQualityProfileId(int qualityProfileId) {
        this.qualityProfileId = qualityProfileId;
    }
}
