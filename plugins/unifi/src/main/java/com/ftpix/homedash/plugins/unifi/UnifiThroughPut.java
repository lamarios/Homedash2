package com.ftpix.homedash.plugins.unifi;

import java.text.DecimalFormat;

public class UnifiThroughPut {
    private Double up, down;

    private int latency;

    private String upHumanReadable, downHumanReadable;
    private String unit = "mbps";
    private String site;

    private UnifiThroughPut(String site, Double up, Double down, int latency) {
        this.site = site;

        this.up = up;
        this.down = down;
        this.latency = latency;

        updateHumanReadable();
    }

    public static UnifiThroughPut of(String site, Double up, Double down, int latency) {
        return new UnifiThroughPut(site, up, down, latency);
    }

    public String getUpHumanReadable() {
        return upHumanReadable;
    }

    public String getDownHumanReadable() {
        return downHumanReadable;
    }

    public Double getUp() {
        return up;
    }

    public int getLatency() {
        return latency;
    }

    public String getUnit() {
        return unit;
    }

    private void updateHumanReadable() {
        //mebibits
        double upH = up / 131072;
        double downH = down / 131072;
        unit = "mbps";


        //to gigibits
        if (upH >= 1000 || downH >= 1000) {
            upH /= 1000;
            downH /= 1000;
            unit = "gbps";
        }

        DecimalFormat format = new DecimalFormat("##0.00");
        upHumanReadable = format.format(upH);
        downHumanReadable = format.format(downH);


        upHumanReadable = formatNumber(upHumanReadable);
        downHumanReadable = formatNumber(downHumanReadable);
    }


    private String formatNumber(String number) {
        if (number.length() > 4) {
            number = number.substring(0, 4);

            if (number.charAt(number.length() - 1) == ',') {
                number = number.substring(0, 5);
            }


        }

        return number;
    }

    public Double getDown() {
        return down;
    }

}
