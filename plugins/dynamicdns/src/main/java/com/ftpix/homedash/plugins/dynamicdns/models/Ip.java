package com.ftpix.homedash.plugins.dynamicdns.models;

import java.util.Date;

/**
 * Created by gz on 15-Jul-16.
 */
public class Ip {
    private String address, method;
    private Date date;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}