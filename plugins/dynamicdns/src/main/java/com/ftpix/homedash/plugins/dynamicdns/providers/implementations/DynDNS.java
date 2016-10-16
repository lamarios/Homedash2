package com.ftpix.homedash.plugins.dynamicdns.providers.implementations;

public class DynDNS extends StandardProvider {

    @Override
    public String getName() {
        return "Dyn.com";
    }

    @Override
    protected String getUrl() {
        return "http://members.dyndns.org/nic/update";
    }

}
