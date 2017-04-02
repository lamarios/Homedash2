package com.ftpix.homedash.plugins.mma.model;

import com.ftpix.sherdogparser.models.Organization;

import java.util.List;

/**
 * Created by gz on 4/2/17.
 */
public class HomeDashOrganization extends Organization {
    private List<HomeDashEvent> homeDashEvents;

    public HomeDashOrganization(Organization org) {
        this.setName(org.getName());
        this.setSherdogUrl(org.getSherdogUrl());
    }

    public List<HomeDashEvent> getHomeDashEvents() {
        return homeDashEvents;
    }

    public void setHomeDashEvents(List<HomeDashEvent> events) {

        this.homeDashEvents = events;
    }
}
