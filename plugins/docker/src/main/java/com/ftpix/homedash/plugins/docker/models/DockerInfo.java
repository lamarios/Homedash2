package com.ftpix.homedash.plugins.docker.models;

import com.ftpix.homedash.Utils.ByteUtils;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerStats;

import java.util.List;

/**
 * Created by gz on 11-Mar-17.
 */
public class DockerInfo {
    public String id, status, memoryUsagePretty;
    public List<String> names;
    public long cpuUsage;
    public long memoryUsage;
    public long memoryLimit;

    public DockerInfo(Container info) {
        names = info.names();
        id = info.id();
        status = info.status();
    }

    public void setStats(ContainerStats stats) {
        memoryUsage = stats.memoryStats().usage();
        memoryLimit = stats.memoryStats().limit();
        memoryUsagePretty = ByteUtils.humanReadableByteCount(memoryUsage, memoryLimit, true);
    }
}
