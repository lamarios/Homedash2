package com.ftpix.homedash.plugins.dynamicdns;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

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

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
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
    private final int OVH = 0, NO_IP = 1, DYN_DNS = 2;

    private final String LAST_UPDATE = "lastUpdate", IP = "ip", PROVIDERS = "providers", COMMAND_FORCE_REFRESH = "forceRefresh", COMMAND_GET_FIELDS = "getFields", COMMAND_ADD_PROVIDER = "addProvider",
            COMMAND_DELETE_PROVIDER = "deleteProvider", METHOD = "method";
    private final String SETTING_NOTIFICATIONS = "notifications", DATA_IP = "ip", DATA_PROVIDERS = "providers", DATA_LAST_DATE = "lastDate";
    private Ip ip = null;
    private List<DynDNSProvider> providers = new ArrayList<>();

    private boolean sendNotifications = false;

    private final Pattern pattern = Pattern.compile("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");


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


        Type listType = new TypeToken<ArrayList<SaveFormatProvider>>() {
        }.getType();

        Optional<ArrayList<SaveFormatProvider>> optionalProviders = getData(DATA_PROVIDERS, listType);


        //getting providers back
        optionalProviders.ifPresent(formattedProviders -> {

            formattedProviders.forEach(saved -> {
                try {
                    Class<?> clazz;
                    clazz = Class.forName(saved.providerClass);

                    Constructor<?> ctor = clazz.getConstructor();
                    DynDNSProvider provider = (DynDNSProvider) ctor.newInstance();
                    provider.setData(saved.data);

                    providers.add(provider);
                } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    logger.info("[DynDNS] Can't create provider of class: [{}]", saved.providerClass);
                }
            });

            logger.info("[DynDNS] Providers loaded size: [{}]", providers.size());


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
        return new String[]{"2x1", "4x3", ModuleLayout.FULL_SCREEN};
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
            case COMMAND_GET_FIELDS:
                response.setMessage(getFields(message));
                break;
            case COMMAND_ADD_PROVIDER:
                if (addProvider(message)) {
                    response.setCommand(WebSocketMessage.COMMAND_SUCCESS);
                    response.setMessage("Provider added !");
                } else {
                    response.setCommand(WebSocketMessage.COMMAND_ERROR);
                    response.setMessage("Provider added, but error while updating IP !");
                }
                response.setExtra(formatProviders());
                break;

            case COMMAND_DELETE_PROVIDER:
                deleteProvider(message);
                response.setCommand(WebSocketMessage.COMMAND_SUCCESS);
                response.setMessage("Provider deleted !");
                response.setExtra(formatProviders());
                break;

            case COMMAND_FORCE_REFRESH:
                try {
                    refreshProviders();
                    response.setCommand(WebSocketMessage.COMMAND_SUCCESS);
                    response.setMessage("Refresh complete");
                    response.setExtra(formatProviders());
                } catch (Exception e) {
                    logger.error("[DynDNS] Error while refreshing providers", e);
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
                if ((this.ip == null || this.ip.getAddress() == null || (pattern.matcher(ip.getAddress()).matches() && !this.ip.getAddress().equalsIgnoreCase(ip.getAddress())))) {
                    this.ip = ip;
                    logger.info("[DynDNS] New IP [{}] updating providers", ip.getAddress());
                    setData(DATA_IP, this.ip);
                    refreshProviders();

                } else {
                    this.ip = ip;
                    logger.info("[DynDNS] IP[{}] is the same or not valid, nothing to do", ip);
                }
            } else {
                logger.info("[DynDNS] no IP key in ip map");
            }
        } catch (Exception e) {
            logger.error("[DynDNS] Can't get external IP", e);
        }
    }

    @Override
    protected Object refresh(String size) throws Exception {

        Map<String, Object> data = new HashMap<String, Object>();
        data.put(IP, ip);
        data.put(PROVIDERS, formatProviders());

        ((List<SaveFormatProvider>) data.get(PROVIDERS)).stream()
                .filter(provider -> provider.data.containsKey("password"))
                .forEach(provider -> {
                    provider.data.remove("password");
                });
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
        return null;
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
    protected Map<String, Object> getSettingsModel() {
        return null;
    }
    /// plugin methods


    /**
     * Gets current ip
     */
    private Ip getIP() throws IllegalStateException, IOException, Exception {
        // Get ip via router first if possible
        GatewayDevice router = null;
        Ip result = new Ip();

        logger.info("[DynDNS] Trying to get external IP via router (more reliable)");
        GatewayDiscover gatewayDiscover = new GatewayDiscover();
        Map<InetAddress, GatewayDevice> gateways = gatewayDiscover.discover();
        router = gatewayDiscover.getValidGateway();

        if (router != null) {
            String ip = router.getExternalIPAddress();
            logger.info("[DynDNS] IP Found via router UPnP {}", ip);
            result.setMethod("router - UPnP");
            result.setAddress(ip);
        }

        return result;

    }


    /**
     * Gets the DNS provider form
     *
     * @param provider name of the provider
     */
    private List<FormInput> getFields(String provider) {
        switch (Integer.parseInt(provider)) {
            case OVH:
                return new OVH().getForm();
            case NO_IP:
                return new NoIP().getForm();
            case DYN_DNS:
                return new DynDNS().getForm();

        }

        return new ArrayList<>();
    }


    /**
     * Adds a new provider
     */
    private boolean addProvider(String command) {
        try {
            logger.info("[DynDNS] Adding provider from data:\n {}", command);
            Type listType = new TypeToken<ArrayList<InputWrapper>>() {
            }.getType();


            ArrayList<InputWrapper> inputs = new GsonBuilder().create().fromJson(command, listType);

            Map<String, String> data = new HashMap<>();

            DynDNSProvider provider = null;
            for (InputWrapper input : inputs) {
                if (input.name.equalsIgnoreCase("ddns-provider")) {
                    switch (Integer.parseInt(input.value)) {
                        case OVH:
                            provider = new OVH();
                            break;
                        case NO_IP:
                            provider = new NoIP();
                            break;
                        case DYN_DNS:
                            provider = new DynDNS();
                            break;
                    }
                } else {
                    data.put(input.name, input.value);
                }
            }

            if (provider != null) {
                provider.setData(data);
                providers.add(provider);
                logger.info("Saving providers: {}", providers.size());

                saveProviders();
            }

            if (ip != null && ip.getAddress() != null) {
                provider.updateIP(ip.getAddress());
            }
            return true;
        } catch (Exception e) {
            logger.error("Error while saving provider", e);
            return false;
        }
    }


    /**
     * Deletes a provider
     */
    private void deleteProvider(String command) {
        int index = -1;
        for (DynDNSProvider provider : providers) {
            if (provider.getId().equalsIgnoreCase(command)) {
                index = providers.indexOf(provider);
            }
        }

        if (index > -1) {


            providers.remove(index);
            saveProviders();
        }
    }


    /**
     * Refresh providers by sending the new IP to the provider
     */
    private void refreshProviders() throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("IP:" + this.ip.getAddress() + "\n\n");
        builder.append("Update report:\n\n");
        logger.info("[DynDNS] Getting external IP...");
        this.ip = getIP();
        logger.info("[DynDNS] IP:{}, updating providers", ip);

        for (DynDNSProvider provider : providers) {
            boolean update = provider.updateIP(this.ip.getAddress());
            logger.info("[DynDNS] Updating [{} - {}], success ? [{}]", provider.getName(), provider.getHostname(), update);
            builder.append("[" + provider.getName() + " - " + provider.getHostname() + "], success ? [" + update + "]\n");
        }

        ip.setDate(new Date());

        if (sendNotifications) {
            Notifications.send("DynDNS", builder.toString());
        }
    }


    /**
     * Formats provider in a more gson friendly type. GSON has issues ith interface serialization
     */
    private List<SaveFormatProvider> formatProviders() {
        List<SaveFormatProvider> formattedProviders = new ArrayList<>();
        providers.forEach(provider -> {
            SaveFormatProvider formatted = new SaveFormatProvider(provider.getClass().getCanonicalName(), provider.getData());
            formatted.data.put("name", provider.getName());
            formatted.data.put("id", provider.getId());
            formattedProviders.add(formatted);
        });

        return formattedProviders;
    }

    private void saveProviders() {
        setData(DATA_PROVIDERS, formatProviders());
    }

// // inner class

    private class SaveFormatProvider {
        public String providerClass = "";
        public Map<String, String> data;

        public SaveFormatProvider(String providerClass, Map<String, String> data) {
            super();
            this.providerClass = providerClass;
            this.data = data;
        }
    }

    private class InputWrapper {
        public String name, value;
    }


}
