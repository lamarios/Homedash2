package com.ftpix.homedash.plugins.kvm;

import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;
import org.libvirt.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class KvmPlugin extends Plugin {

    private static final String SETTINGS_URL = "url", COMMAND_ACTION = "action";


    @Override
    public String getId() {
        return "kvm";
    }

    @Override
    public String getDisplayName() {
        return "KVM / Qemu";
    }

    @Override
    public String getDescription() {
        return "Start and restart KVM / QEMU virtual machines";
    }

    @Override
    public String getExternalLink() {
        return null;
    }

    @Override
    protected void init() {

    }

    @Override
    public String[] getSizes() {
        return new String[]{"1x1", ModuleLayout.FULL_SCREEN};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return 0;
    }

    @Override
    protected WebSocketMessage processCommand(String command, String message, Object extra) {
        WebSocketMessage wsm = new WebSocketMessage();
        try {
            wsm.setCommand(WebSocketMessage.COMMAND_SUCCESS);
            switch (command) {
                case COMMAND_ACTION:
                    VMAction action = gson.fromJson(message, VMAction.class);
                    VMActionResponse response = processVMAction(action);
                    wsm.setMessage(response.message);
                    wsm.setExtra(getAllVMInfo());
                    break;
            }
        } catch (Exception e) {
            wsm.setCommand(WebSocketMessage.COMMAND_ERROR);
            wsm.setMessage(e.getMessage());
            logger().error("Couldn't process command {} with payload {}", command, message, e);
        }

        return wsm;
    }


    @Override
    public void doInBackground() {

    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    protected Object refresh(String size) throws Exception {
        switch (size) {
            case ModuleLayout.SIZE_1x1:
                return countRunningVMs();
            case ModuleLayout.FULL_SCREEN:
                return getAllVMInfo();
        }
        return null;
    }


    @Override
    public int getRefreshRate(String size) {
        switch (size) {
            case ModuleLayout.FULL_SCREEN:
                return ONE_SECOND * 10;
            case ModuleLayout.SIZE_1x1:
            default:
                return ONE_MINUTE;
        }
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> errors = new HashMap<>();
        if (settings.containsKey(SETTINGS_URL)) {
            Connect conn = null;
            try {
                conn = connectToKVM(settings.get(SETTINGS_URL));
                NodeInfo ni = conn.nodeInfo();

            } catch (LibvirtException e) {
                logger().error("Couldn't connect to kvm host {}", settings.get(SETTINGS_URL), e);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (LibvirtException e) {
                        logger().error("Couldn't close connection to kvm host {}", settings.get(SETTINGS_URL), e);
                    }
                }
            }
        } else {
            errors.put("Empty", "Url is empty");
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


    /// PLUGIN METHODS

    /**
     * Connects to the KVM host
     *
     * @param url the url of the host
     * @return the connection object
     * @throws LibvirtException if the connection fails
     */
    private Connect connectToKVM(String url) throws LibvirtException {
        ConnectAuth ca = new ConnectAuthDefault();
        return new Connect(url, ca, 0);

    }

    /**
     * Count the amount of active VMs
     *
     * @return
     */
    private int countRunningVMs() throws LibvirtException {
        Connect connect = connectToKVM(settings.get(SETTINGS_URL));
        try {
            return connect.numOfDomains();
        } finally {
            connect.close();
        }
    }

    /**
     * Gets all the VM and related info
     *
     * @return a list of running and stopped VMs
     * @throws LibvirtException
     */
    public List<VMInfo> getAllVMInfo() throws LibvirtException {

        Connect connect = connectToKVM(settings.get(SETTINGS_URL));
        try {
            //Getting list of running VMs
            Stream<Domain> runningVms = IntStream.of(connect.listDomains())
                    .mapToObj(s -> {
                        try {
                            return connect.domainLookupByID(s);
                        } catch (LibvirtException e) {
                            logger().error("Couldn't get info for domain {}", s, e);
                            return null;
                        }
                    }).filter(Objects::nonNull);


            //Getting the other ones
            Stream<Domain> definedVMs = Stream.of(connect.listDefinedDomains())
                    .map(s -> {
                        try {
                            return connect.domainLookupByName(s);
                        } catch (LibvirtException e) {
                            logger().error("Couldn't get info for domain {}", s, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull);

            //Merging everything
            return Stream.concat(definedVMs, runningVms)
                    .map(d -> {
                        try {
                            VMInfo info = new VMInfo();
                            info.name = d.getName();
                            switch (d.getInfo().state) {
                                case VIR_DOMAIN_BLOCKED:
                                    info.status = "Blocked";
                                    break;
                                case VIR_DOMAIN_CRASHED:
                                    info.status = "Crashed";
                                    break;
                                case VIR_DOMAIN_SHUTOFF:
                                    info.status = "Shutoff";
                                    break;
                                case VIR_DOMAIN_SHUTDOWN:
                                    info.status = "Shutdown";
                                    break;
                                case VIR_DOMAIN_RUNNING:
                                    info.status = "Running";
                                    break;
                                case VIR_DOMAIN_PAUSED:
                                    info.status = "Paused";
                                    break;
                                case VIR_DOMAIN_NOSTATE:
                                    info.status = "No state";
                                    break;
                            }

                            info.id = d.getID();
                            return info;
                        } catch (LibvirtException e) {
                            logger().error("Couldn't get info from domain", e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(VMInfo::getName))
                    .collect(Collectors.toList());
        } finally {
            connect.close();
        }
    }


    /**
     * Process a VM action like start or reboot
     *
     * @param action the action to process
     * @return the response to send back to client
     */
    private VMActionResponse processVMAction(VMAction action) throws LibvirtException {
        VMActionResponse response = new VMActionResponse();
        Connect conn = connectToKVM(settings.get(SETTINGS_URL));

        try {

            Optional<Domain> domainOpt = Optional.ofNullable(conn.domainLookupByName(action.getDomain()));
            if (domainOpt.isPresent()) {
                Domain d = domainOpt.get();
                switch (action.getAction()) {
                    case "pause":
                        d.suspend();
                        response.message = d.getName() + " paused successfully";
                        break;
                    case "shutdown":
                        d.shutdown();
                        response.message = d.getName() + " shutdown successfully";
                        break;
                    case "reboot":
                        d.reboot(0);
                        response.message = d.getName() + " reset successfully";
                        break;
                    case "force-reset":
                        d.destroy();
                        d.create();
                        response.message = d.getName() + "  force reset successfully";
                        break;
                    case "force-off":
                        d.destroy();
                        response.message = d.getName() + " force stopped successfully";
                        break;
                    case "start":
                        d.create();
                        response.message = d.getName() + " started successfully";
                        break;
                    case "resume":
                        d.resume();
                        response.message = d.getName() + " resumed successfully";
                        break;
                }
            }

        } finally {
            conn.close();
        }

        return response;
    }
}