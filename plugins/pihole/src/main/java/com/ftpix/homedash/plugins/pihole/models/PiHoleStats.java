package com.ftpix.homedash.plugins.pihole.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by gz on 11-Jun-16.
 */
public class PiHoleStats {
    @SerializedName(value = "domains_being_blocked")
    private String domainsBeingBlocked;
    @SerializedName(value = "dns_queries_today")
    private String dnsQueriesToday;
    @SerializedName(value = "ads_blocked_today")
    private String adsBlockedToday;

    @SerializedName(value = "ads_percentage_today")
    private double adsPercentageToday;

    public String getDomainsBeingBlocked() {
        return domainsBeingBlocked;
    }

    public void setDomainsBeingBlocked(String domainsBeingBlocked) {
        this.domainsBeingBlocked = domainsBeingBlocked;
    }

    public String getDnsQueriesToday() {
        return dnsQueriesToday;
    }

    public void setDnsQueriesToday(String dnsQueriesToday) {
        this.dnsQueriesToday = dnsQueriesToday;
    }

    public String getAdsBlockedToday() {
        return adsBlockedToday;
    }

    public void setAdsBlockedToday(String adsBlockedToday) {
        this.adsBlockedToday = adsBlockedToday;
    }

    public double getAdsPercentageToday() {
        return adsPercentageToday;
    }

    public void setAdsPercentageToday(double adsPercentageToday) {
        this.adsPercentageToday = adsPercentageToday;
    }

}
