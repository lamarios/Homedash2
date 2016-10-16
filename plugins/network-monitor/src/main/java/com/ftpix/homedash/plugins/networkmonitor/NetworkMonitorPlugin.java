package com.ftpix.homedash.plugins.networkmonitor;

import com.ftpix.homedash.Utils.ByteUtils;
import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.plugins.networkmonitor.models.NetworkInfo;

import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gz on 01-Jul-16.
 */
public class NetworkMonitorPlugin extends Plugin {
    private final String SETTING_INTERFACE = "network-interface";
    private final Sigar sigar = new Sigar();
    private long oldUp = 0, oldDown = 0, oldTime = 0;

    @Override
    public String getId() {
        return "networkmonitor";
    }

    @Override
    public String getDisplayName() {
        return "Network Monitor";
    }

    @Override
    public String getDescription() {
        return "Monitor a network interface from your computer";
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
        return new String[]{"2x1", "3x2"};
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
    protected Object refresh(String size) throws Exception {
        return getNetworkInfo(settings.get(SETTING_INTERFACE).trim());
    }

    @Override
    public int getRefreshRate(String size) {
        return ONE_SECOND * 2;
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> errors = new HashMap<String, String>();

        try {
            NetInterfaceConfig ifConfig = sigar.getNetInterfaceConfig(settings.get(SETTING_INTERFACE).trim());
            if (ifConfig == null) {
                errors.put("Interface", "Interface " + settings.get(SETTING_INTERFACE).trim() + " doesn't exist. Existing interfaces: " + String.join(",", sigar.getNetInterfaceList()));

            }
        } catch (Exception e) {
            try {
                errors.put("Interface", "Interface " + settings.get(SETTING_INTERFACE).trim() + " doesn't exist. Existing interfaces: " + String.join(", ", sigar.getNetInterfaceList()));
            } catch (Exception e2) {
                errors.put("System error", "Unable to get network interface, try to restart HomeDash or your system is incompatible with the network interface monitoring library.");
            }
        }

        return errors;
    }

    @Override
    public ModuleExposedData exposeData() {
        return null;
    }

    @Override
    public Map<String, String> exposeSettings() {
        Map<String, String> exposed = new HashMap<>();
        exposed.put("Interface", settings.get(SETTING_INTERFACE));

        return exposed;
    }


    //////// plugin methods
    public NetworkInfo getNetworkInfo(String netInterface) throws SigarException {

        NetworkInfo networkInfo = new NetworkInfo();
        NetInterfaceConfig ifConfig = sigar.getNetInterfaceConfig(netInterface);

        if (!NetFlags.NULL_HWADDR.equals(ifConfig.getHwaddr())) {
            networkInfo.ip = ifConfig.getAddress();
            networkInfo.name = ifConfig.getName();
        }

        NetInterfaceStat netStat = sigar.getNetInterfaceStat(netInterface);

        long currentTime = System.currentTimeMillis();
        long currentTotalUp = netStat.getTxBytes();
        long currentTotalDown = netStat.getRxBytes();
        if (oldTime > 0) { // we have data let's proceed to calculation
            logger.info("[Network info] We have history, lets calculate the speed since last refresh");
            long transferredUp = currentTotalUp - oldUp;
            long transferredDown = currentTotalDown - oldDown;
            double duration = (currentTime - oldTime) / 1000; // from
            // millisecond
            // to seconds
            logger.info("[Network info] Uploaded [{}] Downloaded [{}] in [{}]s", transferredUp, transferredDown, duration);

            networkInfo.down = (long) Math.ceil(transferredDown / duration);
            networkInfo.up = (long) Math.ceil(transferredUp / duration);

        }

        networkInfo.totalDown = currentTotalDown;
        networkInfo.totalUp = currentTotalUp;

        networkInfo.readableTotalDown = ByteUtils.humanReadableByteCount(networkInfo.totalDown, true);
        networkInfo.readableTotalUp = ByteUtils.humanReadableByteCount(networkInfo.totalUp, true);

        networkInfo.readableDown = ByteUtils.humanReadableByteCount(networkInfo.down, true) + "/s";
        networkInfo.readableUp = ByteUtils.humanReadableByteCount(networkInfo.up, true) + "/s";

        oldTime = currentTime;
        oldUp = currentTotalUp;
        oldDown = currentTotalDown;

        return networkInfo;
    }

}
