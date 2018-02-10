package com.ftpix.homedash.plugins.couchpotato.models;

public class MovieRequest {
    private MovieObject movie;
    private MoviesRootFolder folder;
    private QualityProfile quality;


    public MovieObject getMovie() {
        return movie;
    }

    public void setMovie(MovieObject movie) {
        this.movie = movie;
    }

    public MoviesRootFolder getFolder() {
        return folder;
    }

    public void setFolder(MoviesRootFolder folder) {
        this.folder = folder;
    }

    public QualityProfile getQuality() {
        return quality;
    }

    public void setQuality(QualityProfile quality) {
        this.quality = quality;
    }
}
