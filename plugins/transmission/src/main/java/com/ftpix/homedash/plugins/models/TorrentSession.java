package com.ftpix.homedash.plugins.models;

import ca.benow.transmission.model.SessionStatus;

import java.util.List;

/**
 * Created by gz on 07-Jun-16.
 */
public class TorrentSession {
    public SessionStatus status;
    public boolean alternateSpeeds;
    public List<TorrentObject> torrents;
    public int rpcVersion;
}
