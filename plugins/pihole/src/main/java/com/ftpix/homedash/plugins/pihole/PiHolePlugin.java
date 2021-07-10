package com.ftpix.homedash.plugins.pihole;

import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.plugins.pihole.models.PiHoleStats;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gz on 11-Jun-16.
 */
public class PiHolePlugin extends Plugin {
    private final String SETTING_URL = "url", SETTING_KEY = "key";

    private PiHoleClient client;

    @Override
    public String getId() {
        return "pihole";
    }

    @Override
    public String getDisplayName() {
        return "PiHole";
    }

    @Override
    public String getDescription() {
        return "Get stats from your PiHole";
    }

    @Override
    public String getExternalLink() {
        return client.getUrl();
    }

    @Override
    protected void init() {
        String url = settings.get(SETTING_URL);
        if (!url.endsWith("/")) {
            url += "/";
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        client = new PiHoleClient(url, settings.get(SETTING_KEY));
    }

    @Override
    public int getBackgroundRefreshRate() {
        return NEVER;
    }

    @Override
    public WebSocketMessage processCommand(String command, String message, Object extra) {
        return null;
    }

    @Override
    public void doInBackground() {

    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    protected Object refresh(boolean fullScreen) throws Exception {
        if (fullScreen) {
            return client.getQueries();
        } else {
            return client.getStats();
        }
    }

    @Override
    public int getRefreshRate(boolean fullScreen) {
        if (fullScreen) {
            return ONE_SECOND;
        } else {
            return ONE_MINUTE;
        }

    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> errors = new HashMap<>();

        String url = settings.get(SETTING_URL);
        if (!url.endsWith("/")) {
            url += "/";
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        try {
            HttpResponse<String> response = Unirest.post(url + "api.php").header("cache-control", "no-cache").asString();
            gson.fromJson(response.getBody(), PiHoleStats.class);
        } catch (Exception e) {
            errors.put("Unavailable", "Unable to reach PiHole admin at the following address: " + url + "api.php");
        }


        return errors;

    }

    @Override
    public ModuleExposedData exposeData() {
        try {
            ModuleExposedData data = new ModuleExposedData();
            PiHoleStats stats = client.getStats();

            NumberFormat formatter = new DecimalFormat("#0.00");

            List<String> texts = new ArrayList<>();
            texts.add("Today");
            texts.add("Ads blocked: " + stats.getAdsBlockedToday());
            texts.add("Ads percentage: " + formatter.format(stats.getAdsPercentageToday()));
            texts.add("DNS queries: " + stats.getDnsQueriesToday());
            texts.add("Domains blocked: " + stats.getDomainsBeingBlocked());

            data.setTexts(texts);


            return data;
        } catch (UnirestException e) {
            logger().error("Error while retreiving stats", e);
            return null;
        }
    }

    @Override
    public Map<String, String> exposeSettings() {
        return settings;
    }

    @Override
    protected void onFirstClientConnect() {

    }

    @Override
    protected void onLastClientDisconnect() {

    }

    @Override
    protected Map<String, Object> getSettingsModel() {
        return null;
    }

    @Override
    public boolean hasFullScreen() {
        return true;
    }
    // /////////////////////////////
    // //Plugin Methods

}



