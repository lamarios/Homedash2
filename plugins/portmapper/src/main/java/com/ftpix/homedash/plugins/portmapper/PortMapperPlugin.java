package com.ftpix.homedash.plugins.portmapper;

import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Created by gz on 12-Jun-16.
 */
public class PortMapperPlugin extends Plugin {
    private List<MappingObject> mappings = new ArrayList<>();
    private List<MappingObject> forcedPorts;
    private GatewayDevice router;
    private final String METHOD_GET_ROUTER = "getRouter", METHOD_GET_MAPPINGS = "getMappings", METHOD_ADDPORT = "addPort", METHOD_REMOVE_PORT = "removePort", METHOD_ADDPORTFORCED = "addPortForce",
            SAVE_PORT = "savePort", FORCED_PORTS = "ports";


    @Override
    public String getId() {
        return "portmapper";
    }

    @Override
    public String getDisplayName() {
        return "UPnP Port mapper";
    }

    @Override
    public String getDescription() {
        return "See and control ports opened on your router";
    }

    @Override
    public String getExternalLink() {
        return null;
    }

    @Override
    protected void init() {
        List<MappingObject> ports = new ArrayList<>();

        Map<String, Object> data = getAllData();
        data.forEach((key, value) -> {
            MappingObject mapping = (MappingObject) value;
            ports.add(mapping);
        });

        forcedPorts = ports;
        logger().info("Init with [{}] ports", ports.size());
    }

    @Override
    public String[] getSizes() {
        return new String[]{"2x1", "5x5", "6x5", ModuleLayout.FULL_SCREEN};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return ONE_MINUTE * 5;
    }

    @Override
    public WebSocketMessage processCommand(String command, String message, Object extra) {
        WebSocketMessage response = new WebSocketMessage();
        if (command.equalsIgnoreCase(METHOD_GET_ROUTER)) {
            try {
                getRouter();
                if (router != null) {
                    RouterObject object = new RouterObject();
                    object.name = router.getFriendlyName();
                    object.externalIp = router.getExternalIPAddress();
                    response.setMessage(object);
                    response.setCommand(METHOD_GET_ROUTER);

                } else {
                    response.setMessage("Can't get router information");
                    response.setCommand("error");
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.setMessage("Can't get router information");
                response.setCommand("error");
            }
        } else if (command.equalsIgnoreCase(METHOD_GET_MAPPINGS)) {
            try {
                response.setCommand(METHOD_GET_MAPPINGS);
                response.setMessage(getMappings());
            } catch (Exception e) {
                e.printStackTrace();
                response.setMessage("Can't get mapping information");
                response.setCommand("error");
            }
        } else if (command.equalsIgnoreCase(METHOD_ADDPORT)) {
            try {
                addPort(message, false);
                response.setCommand(METHOD_GET_MAPPINGS);
                response.setMessage(getMappings());
            } catch (Exception e) {
                e.printStackTrace();
                response.setMessage("Error while adding port:" + e.getMessage());
                response.setCommand("error");
            }
        } else if (command.equalsIgnoreCase(METHOD_ADDPORTFORCED)) {
            try {
                addPort(message, true);
                response.setCommand(METHOD_GET_MAPPINGS);
                response.setMessage(getMappings());
            } catch (Exception e) {
                e.printStackTrace();
                response.setMessage("Error while adding port:" + e.getMessage());
                response.setCommand("error");
            }
        } else if (command.equalsIgnoreCase(METHOD_REMOVE_PORT)) {
            try {
                removePort(message);
                response.setCommand(METHOD_GET_MAPPINGS);
                response.setMessage(getMappings());
            } catch (Exception e) {
                e.printStackTrace();
                response.setMessage("Error while Removing port:" + e.getMessage());
                response.setCommand("error");
            }
        } else if (command.equalsIgnoreCase(SAVE_PORT)) {
            try {
                savePort(message);
                response.setCommand(SAVE_PORT);
                response.setMessage(getMappings());
            } catch (Exception e) {
                e.printStackTrace();
                response.setMessage("Error while Saving port:" + e.getMessage());
                response.setCommand("error");
            }
        }
        return response;
    }

    @Override
    public void doInBackground() {
        logger().info("Doing in background");
        try {
            getRouter();
            getMappings();
        } catch (IOException | SAXException e) {
            logger().error("Error while refreshing ports", e);
        }
    }

    @Override
    public boolean hasSettings() {
        return false;
    }

    @Override
    protected Object refresh(String size) throws Exception {
        Map<String, Object> returnValue = new Hashtable<String, Object>();
        try {
            if (this.router != null) {
                RouterObject object = new RouterObject();
                object.name = router.getFriendlyName();
                object.externalIp = router.getExternalIPAddress();
                returnValue.put("router", object);
            }

        } catch (Exception e) {
            logger().info("Can't get router info");
        }

        returnValue.put("mappings", mappings);
        return returnValue;
    }

    @Override
    public int getRefreshRate(String size) {
        return ONE_SECOND * 10;
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        return null;
    }

    @Override
    public ModuleExposedData exposeData() {
        try {
            ModuleExposedData data = new ModuleExposedData();
            if (router != null) {
                data.addText(router.getExternalIPAddress());
                data.addText(mappings.size() + " ports open");
                return data;
            }
            return null;
        } catch (Exception e) {
            logger().error("Couldn't get port mapper exposed data", e);
            return null;
        }
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
    /////////////////////////////////
    ///// plugin methods


    private void removePort(String message) throws NumberFormatException, IOException, SAXException {
        if (router != null) {
            logger().info("Removing port:{}", message);
            String split[] = message.split("\\|");
            router.deletePortMapping(Integer.parseInt(split[0]), split[1]);
            MappingObject mapping = new MappingObject();
            mapping.externalPort = Integer.parseInt(split[0]);
            mapping.protocol = split[1];
            mapping.forced = true;

            if (forcedPorts.contains(mapping)) {
                logger().info("Port in saved list, removing it");
                forcedPorts.remove(mapping);
                removeData(mapping.toString());
            }
        }
    }

    private void savePort(String message) {
        logger().info("Saving port:{}", message);
        String split[] = message.split("\\|");

        MappingObject mapping = new MappingObject();
        mapping.forced = true;
        mapping.protocol = split[1];
        mapping.externalPort = Integer.parseInt(split[0]);
        mapping.internalPort = Integer.parseInt(split[0]);
        mapping.internalIp = split[2];
        mapping.name = split[3];

        forcedPorts.add(mapping);
        setData(mapping.toString(), mapping);
        logger().info("Added ports to forced ports. Size: {}", forcedPorts.size());

    }

    private void addPort(String message, boolean forced) throws NumberFormatException, IOException, SAXException {
        if (router != null) {
            logger().info("Adding port:{}", message);
            String[] split = message.split("\\|");
            router.addPortMapping(Integer.parseInt(split[2]), Integer.parseInt(split[3]), split[4], split[1], split[0]);

            if (forced) {
                MappingObject obj = new MappingObject();
                obj.externalPort = Integer.parseInt(split[2]);
                obj.internalPort = Integer.parseInt(split[3]);
                obj.internalIp = split[4];
                obj.protocol = split[1];
                obj.name = split[0];
                obj.forced = true;
                if (!forcedPorts.contains(obj)) {
                    forcedPorts.add(obj);
                    logger().info("Added ports to forced ports. Size: {}", forcedPorts.size());
                    setData(obj.toString(), obj);
                }
            }
        }
    }


    private List<MappingObject> getMappings() throws IOException, SAXException {
        logger().info("Refreshing Mapping");
        List<MappingObject> result = new ArrayList<MappingObject>();
        if (router != null) {
            // Integer portMapCount = router.getPortMappingNumberOfEntries();

            PortMappingEntry portMapping = new PortMappingEntry();
            int pmCount = 0;
            do {
                if (router.getGenericPortMappingEntry(pmCount, portMapping)) {
                    logger().info("Portmapping #" + pmCount + " successfully retrieved (" + portMapping.getPortMappingDescription() + ":" + portMapping.getExternalPort() + ")");
                    MappingObject object = new MappingObject();
                    object.externalPort = portMapping.getExternalPort();
                    object.internalPort = portMapping.getInternalPort();
                    object.internalIp = portMapping.getInternalClient();
                    object.name = portMapping.getPortMappingDescription();
                    object.protocol = portMapping.getProtocol();

                    result.add(object);
                    // portMapping = new PortMappingEntry();
                } else {
                    logger().info("Portmapping #" + pmCount + " retrieval failed");
                    break;
                }
                pmCount++;
            } while (portMapping != null);

            // Checking against forced ports
            for (MappingObject mapping : forcedPorts) {
                if (!result.contains(mapping)) {
                    logger().info("Mapping {} on port {} missing", mapping.protocol, mapping.externalPort);
                    router.addPortMapping(mapping.externalPort, mapping.internalPort, mapping.internalIp, mapping.protocol, mapping.name);
                    result.add(mapping);
                } else {
                    int index = result.indexOf(mapping);
                    result.get(index).forced = true;
                }
            }
        } else {
            logger().info("No router yet");

        }

        if (result != null) {
            this.mappings = result;
        }
        return result;
    }

    /**
     * Getting router info
     */
    private void getRouter() {
        try {
            GatewayDiscover gatewayDiscover = new GatewayDiscover();
            Map<InetAddress, GatewayDevice> gateways = gatewayDiscover.discover();
            GatewayDevice router = gatewayDiscover.getValidGateway();

            if (router != null) {
                this.router = router;
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            logger().error("Error while getting router", e);
        }
    }

}
