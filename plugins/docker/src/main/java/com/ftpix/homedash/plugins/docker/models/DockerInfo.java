package com.ftpix.homedash.plugins.docker.models;

import com.ftpix.homedash.Utils.ByteUtils;
import com.google.common.collect.Collections2;
import com.jcraft.jsch.Logger;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerStats;
import com.spotify.docker.client.messages.NetworkStats;
import edu.emory.mathcs.backport.java.util.Collections;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

/**
 * Created by gz on 11-Mar-17.
 */
public class DockerInfo {
    public String id, status, memoryUsagePretty, image, imageId;
    public List<String> names;
    public long memoryUsage = 0;
    public long memoryLimit = 0;
    public long bytesReceived = 0, bytesSent = 0;
    public String bytesReceivedPretty = "N/A", bytesSentPretty = "N/A";

    public DockerInfo(Container info) {
        names = info.names();
        id = info.id();
        status = info.status();
        image = info.image();
        imageId = info.imageId();
    }

    public void setStats(ContainerStats stats) {

        Optional.ofNullable(stats.networks()).ifPresent(map -> {
            map.forEach((k, v) -> {
                bytesSent += v.txBytes();
                bytesReceived += v.rxBytes();
            });
        });

        if (bytesReceived > 0) {
            bytesReceivedPretty = ByteUtils.humanReadableByteCount(bytesReceived, true);
        }

        if (bytesSent > 0) {
            bytesSentPretty = ByteUtils.humanReadableByteCount(bytesSent, true);
        }

        memoryUsage = stats.memoryStats().usage();
        memoryLimit = stats.memoryStats().limit();
        memoryUsagePretty = ByteUtils.humanReadableByteCount(memoryUsage, memoryLimit, true);
    }
}
