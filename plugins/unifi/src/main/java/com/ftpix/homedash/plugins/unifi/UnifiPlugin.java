package com.ftpix.homedash.plugins.unifi;

import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;

import java.util.HashMap;
import java.util.Map;

public class UnifiPlugin extends Plugin {

    private final static String SETTING_USERNAME = "username";
    private final static String SETTING_PASSWORD = "password";
    private final static String SETTING_URL = "url";
    private final static String SETTING_SITE = "site";


    private String username, password;
    private UnifiApi api;


    @Override
    public String getId() {
        return "unifi";
    }

    @Override
    public String getDisplayName() {
        return "Unifi throughput";
    }

    @Override
    public String getDescription() {
        return "Gets a Unifi site current throughput";
    }

    @Override
    public String getExternalLink() {
        return null;
    }

    @Override
    protected void init() {

        api = new UnifiApi(transformUrl(settings.get(SETTING_URL)), settings.get(SETTING_SITE));

        username = settings.get(SETTING_USERNAME);
        password = settings.get(SETTING_PASSWORD);
    }

    @Override
    public int getBackgroundRefreshRate() {
        return 0;
    }

    @Override
    protected WebSocketMessage processCommand(String command, String message, Object extra) {
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

        if (api.login(username, password)) {
            return api.getThroughput();
        }

        throw new Exception("Couldn't login to unifi controller");

    }

    @Override
    public int getRefreshRate(boolean fullScreen) {
        return ONE_SECOND * 3;
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> errors = new HashMap<>();


        UnifiApi api = new UnifiApi(transformUrl(settings.get(SETTING_URL)), settings.get(SETTING_SITE));

        try {
            if (api.login(settings.get(SETTING_USERNAME), settings.get(SETTING_PASSWORD))) {
                api.getThroughput();
            } else {
                errors.put("Login failed", "Username or password is incorrect");
            }


        } catch (Exception e) {
            logger().error("Couldn't login to controller");
            errors.put("Login error", e.getMessage());
        }


        return errors;
    }

    @Override
    public ModuleExposedData exposeData() {
        return null;
    }

    @Override
    public Map<String, String> exposeSettings() {
        return null;
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
        return false;
    }


    ////////////////////////
    //// plugin methods
    ///////////////////////


    /**
     * Tries to format a URL accordingly to plugin needs
     *
     * @param url the url to format
     * @return url with missing componenets if any
     */
    private String transformUrl(String url) {
        if (!url.startsWith("http")) {
            url = "http://" + url;
        }

        if (!url.endsWith("/")) {
            url += "/";
        }

        return url;
    }


}
