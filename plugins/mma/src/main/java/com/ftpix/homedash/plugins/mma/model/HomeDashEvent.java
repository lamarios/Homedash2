package com.ftpix.homedash.plugins.mma.model;


import com.ftpix.sherdogparser.models.Event;

/**
 * Created by gz on 4/2/17.
 */
public class HomeDashEvent extends Event {

    private String mainEventPhoto1, mainEventPhoto2;

    public HomeDashEvent(Event event) {
        this.setDate(event.getDate());
        this.setFights(event.getFights());
        this.setLocation(event.getLocation());
        this.setName(event.getName());
        this.setOrganization(event.getOrganization());
        this.setSherdogUrl(event.getSherdogUrl());
    }


    public String getMainEventPhoto1() {
        return mainEventPhoto1;
    }

    public void setMainEventPhoto1(String mainEventPhoto1) {
        this.mainEventPhoto1 = mainEventPhoto1;
    }

    public String getMainEventPhoto2() {
        return mainEventPhoto2;
    }

    public void setMainEventPhoto2(String mainEventPhoto2) {
        this.mainEventPhoto2 = mainEventPhoto2;
    }
}
