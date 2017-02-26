package com.ftpix.homedash.plugins.harddisk;


import com.ftpix.homedash.Utils.ByteUtils;
import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;

import java.io.File;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;

/**
 * Created by gz on 06-Jun-16.
 */
public class HarddiskPlugin extends Plugin {
    private final String UUID = "uuid";
    private final static int MAX_DATA = 100;
    private SystemInfo systemInfo = new SystemInfo();
    private HWPartition partition;
    private HWDiskStore disk;
    private List<HardDiskInfo> data = new LinkedList<>();
    private long maxSpeed = 0;

    @Override
    public String getId() {
        return "harddisk";
    }

    @Override
    public String getDisplayName() {
        return "Hard Disk";
    }

    @Override
    public String getDescription() {
        return "Help you monitor the space on a mount point";
    }

    @Override
    public String getExternalLink() {
        return null;
    }

    @Override
    protected void init() {
        refreshDisk();
        Stream.of(disk.getPartitions())
                .filter(p -> p.getUuid().equalsIgnoreCase(settings.get(UUID)))
                .findFirst()
                .ifPresent(p -> partition = p);
    }

    @Override
    public String[] getSizes() {
        return new String[]{"1x1", "2x1"};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return ONE_SECOND * 3;
    }

    @Override
    public WebSocketMessage processCommand(String command, String message, Object extra) {
        return null;
    }

    @Override
    public void doInBackground() {
        refreshDisk();
        data.add(getDriveInfo());
        if (data.size() > MAX_DATA) {
            data.remove(0);
        }

    }

    @Override
    protected Object refresh(String size) throws Exception {
        File root = new File(partition.getMountPoint());

        long usedSpace = root.getTotalSpace() - root.getFreeSpace();


        Map<String, Object> spaces = new Hashtable<>();

        spaces.put("path", root.getAbsolutePath());
        spaces.put("total", Long.toString(root.getTotalSpace()));
        spaces.put("free", Long.toString(root.getFreeSpace()));
        spaces.put("used", Long.toString(usedSpace));
        spaces.put("pretty", ByteUtils.humanReadableByteCount(usedSpace, root.getTotalSpace(), true));

        if (size.equalsIgnoreCase(ModuleLayout.FULL_SCREEN)) {
            spaces.put("data", data);
        }

        if (!data.isEmpty() && maxSpeed > 0) {
            HardDiskInfo info = data.get(data.size() - 1);
            long speedToCheck = info.readSpeed + info.writeSpeed;
            double percOfMax = ((double) speedToCheck / (double) maxSpeed);
            spaces.put("usage", percOfMax);
        }

        return spaces;
    }

    @Override
    public int getRefreshRate(String size) {
        return ONE_SECOND * 3;
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> errors = new Hashtable<>();

        Stream.of(systemInfo.getHardware().getDiskStores())
                .flatMap(ds -> Stream.of(ds.getPartitions()))
                .filter(p -> p.getUuid().equalsIgnoreCase(settings.get(UUID)))
                .findFirst()
                .ifPresent(p -> {
                    if (!new File(p.getMountPoint()).exists()) {
                        errors.put("Path", "This mount point doesn't exist.");
                    }
                });

        return errors;
    }

    @Override
    public ModuleExposedData exposeData() {
        ModuleExposedData data = new ModuleExposedData();

        File root = new File(partition.getMountPoint());
        long usedSpace = root.getTotalSpace() - root.getFreeSpace();

        data.addText(root.getAbsolutePath());
        data.addText(ByteUtils.humanReadableByteCount(usedSpace, root.getTotalSpace(), true));
        return data;
    }

    @Override
    public Map<String, String> exposeSettings() {
        Map<String, String> result = new Hashtable<>();
        result.put("Path", partition.getMountPoint());
        return result;
    }

    @Override
    protected Map<String, Object> getSettingsModel() {

        return Stream.of(systemInfo.getHardware().getDiskStores())
                .flatMap(ds -> Stream.of(ds.getPartitions()))
                .filter(ds -> ds.getMountPoint().trim().length() > 0)
                .collect(Collectors.toMap(HWPartition::getUuid, Function.identity()));
    }


    /**
     * Refresh the disk so that we can get updated data
     */
    private void refreshDisk() {
        Stream.of(systemInfo.getHardware().getDiskStores())
                .filter(ds -> Stream.of(ds.getPartitions())
                        .anyMatch(p -> p.getUuid().equalsIgnoreCase(settings.get(UUID)))
                ).findFirst().ifPresent(ds -> this.disk = ds);
    }

    //plugin method
    private HardDiskInfo getDriveInfo() {
        HardDiskInfo info = new HardDiskInfo();

        info.readTotal = disk.getReadBytes();
        info.time = System.currentTimeMillis();
        info.writeTotal = disk.getWriteBytes();


        if (data.size() > 0) {
            HardDiskInfo previous = data.get(data.size() - 1);

            long writeDiff = info.writeTotal - previous.writeTotal;
            long readDiff = info.readTotal - previous.readTotal;

            //diff in seconds
            long timeDiff = (info.time - previous.time) / 1000;

            info.writeSpeed = writeDiff / timeDiff;
            info.readSpeed = readDiff / timeDiff;

            maxSpeed = Math.max(info.writeSpeed + info.readSpeed, maxSpeed);

            info.readSpeedPretty = ByteUtils.humanReadableByteCount(info.readSpeed, true);
            info.writeSpeedPretty = ByteUtils.humanReadableByteCount(info.writeSpeed, true);
            info.writeTotalPretty = ByteUtils.humanReadableByteCount(info.writeTotal, true);
            info.readTotalPretty = ByteUtils.humanReadableByteCount(info.readTotal, true);
            logger.info("Write Speed [{}/s], read Speed [{}/s], max Speed [{}/s]", info.writeSpeedPretty, info.readSpeedPretty, ByteUtils.humanReadableByteCount(maxSpeed, true));
        }

        return info;
    }


}
