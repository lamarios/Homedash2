package com.ftpix.homedash.plugins.models;

import ca.benow.transmission.model.TorrentStatus;

/**
 * Created by gz on 07-Jun-16.
 */
public class TorrentObject {


    public String name;
    public String statusStr;
    public int downloadSpeed, uploadSpeed, id, status;
    public double percentDone;
    public long downloaded, uploaded, totalSize, seedRatioLimit;

    public void mapTorrent(TorrentStatus torrent, int rpcVersion) {

        name = torrent.getName();
        downloadSpeed = Integer.parseInt(torrent.getField(TorrentStatus.TorrentField.rateDownload).toString());
        uploadSpeed = Integer.parseInt(torrent.getField(TorrentStatus.TorrentField.rateUpload).toString());
        status = Integer.parseInt(torrent.getField(TorrentStatus.TorrentField.status).toString());

        statusStr = TorrentStatus.getStatusString(status, rpcVersion);

        percentDone = Double.parseDouble(torrent.getField(TorrentStatus.TorrentField.percentDone).toString());
        id = torrent.getId();
        downloaded = Long.parseLong(torrent.getField(TorrentStatus.TorrentField.downloadedEver).toString());
        uploaded = Long.parseLong(torrent.getField(TorrentStatus.TorrentField.uploadedEver).toString());
        totalSize = Long.parseLong(torrent.getField(TorrentStatus.TorrentField.totalSize).toString());

        seedRatioLimit = Long.parseLong(torrent.getField(TorrentStatus.TorrentField.seedRatioLimit).toString());
    }
}
