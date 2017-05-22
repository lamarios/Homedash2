package com.ftpix.homedash.plugins.networkmonitor;

import com.ftpix.homedash.Utils.ByteUtils;
import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.plugins.networkmonitor.models.NetworkInfo;
import oshi.SystemInfo;
import oshi.hardware.NetworkIF;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by gz on 01-Jul-16.
 */
public class NetworkMonitorPlugin extends Plugin {
  private final String SETTING_INTERFACE = "network-interface";
  private final int MAX_SIZE = 25;
  private SystemInfo systemInfo = new SystemInfo();
  private List<NetworkInfo> networkInfos = new LinkedList<>();

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
    return ONE_SECOND * 2;
  }

  @Override
  protected WebSocketMessage processCommand(String command, String message, Object extra) {
    return null;
  }

  @Override
  public void doInBackground() {
    networkInfos.add(getNetworkInfo(settings.get(SETTING_INTERFACE)));
    if (networkInfos.size() > MAX_SIZE) {
      networkInfos.remove(0);
    }
  }

  @Override
  protected Object refresh(String size) throws Exception {
    return networkInfos;
  }

  @Override
  public int getRefreshRate(String size) {
    return ONE_SECOND * 2;
  }

  @Override
  public Map<String, String> validateSettings(Map<String, String> settings) {
    Map<String, String> errors = new HashMap<String, String>();

    NetworkIF[] interfaces = systemInfo.getHardware().getNetworkIFs();

    try {
      Optional<NetworkIF> ifConfigOpt = Stream.of(interfaces)
          .filter(iface -> iface.getName().equalsIgnoreCase(settings.get(SETTING_INTERFACE).trim())).findFirst();
      if (!ifConfigOpt.isPresent()) {
        errors.put("Interface", "Interface " + settings.get(SETTING_INTERFACE).trim() + " doesn't exist. Existing interfaces: " + Stream.of(interfaces).map(NetworkIF::getName).collect(Collectors.joining(",")));

      }
    } catch (Exception e) {
      try {
        errors.put("Interface", "Interface " + settings.get(SETTING_INTERFACE).trim() + " doesn't exist. Existing interfaces: " + Stream.of(interfaces).map(NetworkIF::getName).collect(Collectors.joining(",")));
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

  @Override
  protected Map<String, Object> getSettingsModel() {
    return Stream.of(systemInfo.getHardware().getNetworkIFs()).collect(Collectors.toMap(NetworkIF::getName, netIf -> String.join(", ", netIf.getIPv4addr())));
  }

  //////// plugin methods
  public NetworkInfo getNetworkInfo(String netInterface) {


    Optional<NetworkIF> ifConfigOpt = Stream.of(systemInfo.getHardware().getNetworkIFs())
        .filter(iface -> iface.getName().equalsIgnoreCase(netInterface)).findFirst();
    NetworkInfo networkInfo = new NetworkInfo();
    if (ifConfigOpt.isPresent()) {
      NetworkIF ifConfig = ifConfigOpt.get();

      networkInfo.ip = ifConfig.getIPv4addr()[0];
      networkInfo.name = ifConfig.getName();


      long currentTime = System.currentTimeMillis();
      long currentTotalUp = ifConfig.getBytesSent();
      long currentTotalDown = ifConfig.getBytesRecv();


      if (!networkInfos.isEmpty()) { // we have data let's proceed to calculation
        NetworkInfo old = networkInfos.get(networkInfos.size() - 1);

        if (old.totalDown > 0 && old.totalUp > 0) {
          logger.info("[Network info] We have history, lets calculate the speed since last refresh");
          long transferredUp = currentTotalUp - old.totalUp;
          long transferredDown = currentTotalDown - old.totalDown;
          double duration = (currentTime - old.time) / 1000; // from
          if (duration == 0) duration = duration + 0.1;
          // millisecond
          // to seconds
          logger.info("[Network info] Uploaded [{}] Downloaded [{}] in [{}]s", transferredUp, transferredDown, duration);

          networkInfo.down = (long) Math.ceil(transferredDown / duration);
          networkInfo.up = (long) Math.ceil(transferredUp / duration);

          networkInfo.readableDown = ByteUtils.humanReadableByteCount(networkInfo.down, true) + "/s";
          networkInfo.readableUp = ByteUtils.humanReadableByteCount(networkInfo.up, true) + "/s";
          logger.info("[Network Info] Upload: {}. download: {}", networkInfo.readableUp, networkInfo.readableDown);
        }


      }
      networkInfo.time = currentTime;

      networkInfo.totalDown = currentTotalDown;
      networkInfo.totalUp = currentTotalUp;

      networkInfo.readableTotalDown = ByteUtils.humanReadableByteCount(networkInfo.totalDown, true);
      networkInfo.readableTotalUp = ByteUtils.humanReadableByteCount(networkInfo.totalUp, true);


      return networkInfo;
    } else {
      return null;
    }
  }

}
