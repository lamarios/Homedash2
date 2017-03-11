package com.ftpix.homedash.plugins.docker.models;

import com.spotify.docker.client.messages.Container;

/**
 * Created by gz on 11-Mar-17.
 */
public class DockerExtendedInfo extends DockerInfo {

    public DockerExtendedInfo(Container info) {
        super(info);
    }
}
