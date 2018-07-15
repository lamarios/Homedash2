package com.ftpix.homedash.plugins.harddisk;

/**
 * Created by gz on 5/6/17.
 */
public class FileOperation {
    private String source, destination;
    int progress = 0;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {

        this.destination = destination;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}
