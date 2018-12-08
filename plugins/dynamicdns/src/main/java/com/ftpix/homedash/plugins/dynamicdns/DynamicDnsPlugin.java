package com.ftpix.homedash.plugins.dynamicdns;

import com.ftpix.homedash.plugins.dynamicdns.models.IpFromWeb;
import com.google.gson.Gson;

import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.notifications.Notifications;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.plugins.dynamicdns.inputs.FormInput;
import com.ftpix.homedash.plugins.dynamicdns.models.Ip;
import com.ftpix.homedash.plugins.dynamicdns.providers.DynDNSProvider;
import com.ftpix.homedash.plugins.dynamicdns.providers.implementations.DynDNS;
import com.ftpix.homedash.plugins.dynamicdns.providers.implementations.NoIP;
import com.ftpix.homedash.plugins.dynamicdns.providers.implementations.OVH;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.gsonfire.GsonFireBuilder;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Created by gz on 15-Jul-16.
 */
public class DynamicDnsPlugin extends Plugin {

    private final String IP = "ip", COMMAND_FORCE_REFRESH = "forceRefresh",
            SETTINGS_PROVIDER = "provider";
    private final String SETTING_NOTIFICATIONS = "notifications", DATA_IP = "ip";
    private Ip ip = null;
    private DynDNSProvider provider;

    private final static String IP_URL = "https://api.ipify.org/?format=json";


    private boolean sendNotifications = false;

    private final static Pattern PATTERN = Pattern.compile("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");


    @Override
    public String getId() {
        return "dynamicdns";
    }

    @Override
    public String getDisplayName() {
        return "Dynamic DNS";
    }

    @Override
    public String getDescription() {
        return "Manage your dynamic dns entries from various providers.";
    }

    @Override
    public String getExternalLink() {
        return null;
    }

    @Override
    protected void init() {

        //getting providers back
        Optional.ofNullable(settings.get(SETTINGS_PROVIDER)).ifPresent(classStr -> {

            try {
                Class<?> clazz;
                clazz = Class.forName(classStr);

                Constructor<?> ctor = clazz.getConstructor();
                DynDNSProvider provider = (DynDNSProvider) ctor.newInstance();
                provider.setData(settings);

                this.provider = provider;
            } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                logger().info("[DynDNS] Can't create provider of class: [{}]", classStr);
            }

            logger().info("[DynDNS] Provider loaded class [{}]", provider.getClass().getCanonicalName());


        });

        if (settings.containsKey(SETTING_NOTIFICATIONS)) {
            sendNotifications = true;
        }

        Optional<Ip> optionalIp = getData(DATA_IP, Ip.class);

        optionalIp.ifPresent(ip -> {
            this.ip = ip;
        });

    }

    @Override
    public String[] getSizes() {
        return new String[]{"2x1"};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return ONE_MINUTE;
    }

    @Override
    protected WebSocketMessage processCommand(String command, String message, Object extra) {
        WebSocketMessage response = new WebSocketMessage();
        response.setCommand(command);
        switch (command) {

            case COMMAND_FORCE_REFRESH:
                try {
                    refreshProviders();
                    response.setCommand(WebSocketMessage.COMMAND_SUCCESS);
                    response.setMessage("Refresh complete");
                } catch (Exception e) {
                    logger().error("[DynDNS] Error while refreshing providers", e);
                    response.setCommand(WebSocketMessage.COMMAND_ERROR);
                    response.setMessage("Error while refreshing IP: " + e.getMessage());
                }
                break;

        }
        return response;
    }

    @Override
    public void doInBackground() {
        try {

            Ip ip = getIP();
            if (ip.getAddress() != null) {
                if ((this.ip == null || this.ip.getAddress() == null || (PATTERN.matcher(ip.getAddress()).matches() && !this.ip.getAddress().equalsIgnoreCase(ip.getAddress())))) {
                    ip.setDate(new Date());
                    this.ip = ip;
                    logger().info("[DynDNS] New IP [{}] updating providers", ip.getAddress());
                    setData(DATA_IP, this.ip);
                    refreshProviders();

                } else {
                    this.ip = ip;
                    logger().info("[DynDNS] IP[{}] is the same or not valid, nothing to do", ip);
                }
            } else {
                logger().info("[DynDNS] no IP key in ip map");
            }
        } catch (Exception e) {
            logger().error("[DynDNS] Can't get external IP", e);
        }
    }

    @Override
    protected Object refresh(String size) throws Exception {

        Map<String, Object> data = new HashMap<String, Object>();
        data.put(IP, ip);
        data.put("host", provider.getHostname());
        data.put("provider", provider.getName());
        return data;
    }

    @Override
    public int getRefreshRate(String size) {
        if (size.equalsIgnoreCase(ModuleLayout.FULL_SCREEN)) {
            return ONE_SECOND * 3;
        } else {
            return ONE_MINUTE;
        }
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        HashMap<String, String> error = new HashMap<>();

        if (settings.get(SETTINGS_PROVIDER).equalsIgnoreCase("-1")) {
            error.put("Provider", "Please select a provider");
        }
        return error;
    }

    @Override
    public ModuleExposedData exposeData() {
        if (ip != null) {
            ModuleExposedData data = new ModuleExposedData();
            data.addText(ip.getAddress());
            data.addText("Last IP Update");
            data.addText(ip.getDate().toString());
            return data;
        }
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
    /// plugin methods


    @Override
    public Object getSettingsScreenData(Map<String, String> settings) {
        DynDNSProvider[] providers = new DynDNSProvider[]{new DynDNS(), new NoIP(), new OVH()};

        Gson gson = new GsonFireBuilder()
                .enableExposeMethodResult()
                .createGsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .disableHtmlEscaping()
                .create();


        Map<String, Object> map = new HashMap<>();
        map.put("object", providers);
        map.put("json", gson.toJson(providers));

        if (settings != null) {
            map.put("settingsJson", gson.toJson(settings));
        }

        return map;
    }

    /**
     * Gets current ip
     */
    private Ip getIP() throws Exception {

        try {
            // Get ip via router first if possible
            GatewayDevice router = null;
            Ip result = new Ip();

            logger().info("[DynDNS] Trying to get external IP via router (more reliable)");
            GatewayDiscover gatewayDiscover = new GatewayDiscover();
            Map<InetAddress, GatewayDevice> gateways = gatewayDiscover.discover();
            router = gatewayDiscover.getValidGateway();

            if (router != null) {
                String ip = router.getExternalIPAddress();
                logger().info("[DynDNS] IP Found via router UPnP {}", ip);
                result.setMethod("router - UPnP");
                result.setAddress(ip);
            } else {
                return getIpFromWeb();
            }

            return result;
        } catch (Exception e) {
            return getIpFromWeb();
        }


    }

    /**
     * Get ip from the web
     *
     * @return
     */
    private Ip getIpFromWeb() throws UnirestException {
        HttpResponse<String> response = Unirest.get(IP_URL)
                .asString();
        IpFromWeb ipFromWeb = gson.fromJson(response.getBody(), IpFromWeb.class);
        logger().info("Getting Ip from web:{}", ipFromWeb.getIp());

        Ip ip = new Ip();
        ip.setAddress(ipFromWeb.getIp());
        ip.setMethod("ipify.com");
        return ip;
    }


    /**
     * Refresh providers by sending the new IP to the provider
     */
    private void refreshProviders() throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("IP:" + this.ip.getAddress() + "\n\n");
        builder.append("Update report:\n\n");
        logger().info("[DynDNS] Getting external IP...");
        this.ip = getIP();
        logger().info("[DynDNS] IP:{}, updating providers", ip);

        boolean update = provider.updateIP(this.ip.getAddress());
        logger().info("[DynDNS] Updating [{} - {}], success ? [{}]", provider.getName(), provider.getHostname(), update);
        builder.append("[" + provider.getName() + " - " + provider.getHostname() + "], success ? [" + update + "]\n");

        ip.setDate(new Date());

        if (sendNotifications) {
            Notifications.send("DynDNS", builder.toString());
        }
    }

}
