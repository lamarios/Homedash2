package com.ftpix.homedash.plugins;

import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.models.CpuInfo;
import com.ftpix.homedash.plugins.models.HardwareInfo;
import com.ftpix.homedash.plugins.models.RamInfo;
import com.ftpix.homedash.plugins.models.SystemInfoData;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.Sensors;

public class SystemInfoPlugin extends Plugin {

    private List<CpuInfo> cpuInfo = new ArrayList<CpuInfo>();
    private List<RamInfo> ramInfo = new ArrayList<RamInfo>();
    private HardwareInfo hardwareInfo = new HardwareInfo();

    private final int MAX_INFO_SIZE = 100, WARNING_THRESHOLD = 90;
    private final String SETTING_NOTIFICATIONS = "notifications";
    private final DecimalFormat nf = new DecimalFormat("#,###,###,##0.00");
    private final SystemInfo systemInfo = new SystemInfo();

    public SystemInfoPlugin() {

    }

    public SystemInfoPlugin(Module module) {
        super(module);
    }

    @Override
    public String getId() {
        return "systeminfo";
    }

    @Override
    public String getDisplayName() {
        return "System Info";
    }

    @Override
    public String getDescription() {
        return "Monitor CPU and RAM usage";
    }

    @Override
    public String getExternalLink() {
        return null;
    }

    @Override
    protected void init() {
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        hardwareInfo.family = processor.getFamily();
        hardwareInfo.identifier = processor.getIdentifier();
        hardwareInfo.logicalCores = processor.getLogicalProcessorCount();
        hardwareInfo.physicalCores = processor.getPhysicalProcessorCount();
        hardwareInfo.is64 = processor.isCpu64bit();
        hardwareInfo.model = processor.getModel();
        hardwareInfo.name = processor.getName();
        hardwareInfo.uptime = processor.getSystemUptime();
        hardwareInfo.vendor = processor.getVendor();
    }

    @Override
    public String[] getSizes() {
        return new String[]{ModuleLayout.FULL_SCREEN, ModuleLayout.SIZE_2x1, ModuleLayout.SIZE_1x1};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return ONE_SECOND * 3;
    }

    @Override
    public int getRefreshRate(String size) {
        return ONE_SECOND * 3;
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        return null;
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
    public void doInBackground() {
        try {
            CpuInfo cpu = getCPUInfo();
            RamInfo ram = getRamInfo();


            if (settings.containsKey(SETTING_NOTIFICATIONS) && cpuInfo.size() > 0 && ramInfo.size() > 0) {
                CpuInfo oldCpu = cpuInfo.get(cpuInfo.size() - 1);
                RamInfo oldRam = ramInfo.get(ramInfo.size() - 1);

                if ((oldCpu.cpuUsage < WARNING_THRESHOLD && cpu.cpuUsage >= WARNING_THRESHOLD) || (oldRam.percentageUsed < WARNING_THRESHOLD && ram.percentageUsed >= WARNING_THRESHOLD)) {
                    logger.debug("Sending high load warning");
                    //Notifications.send("Warning",
                    //		"CPU load (" + nf.format(cpu.cpuUsage) + "%) or Ram load (" + nf.format(ram.percentageUsed) + "%)  became over " + WARNING_THRESHOLD + "%.\n Date: " + new Date());
                }
            }

            logger.info("CPU load:{}%, RAM load:{}%", cpu.cpuUsage, ram.percentageUsed);
            cpuInfo.add(cpu);
            ramInfo.add(ram);

            if (cpuInfo.size() > MAX_INFO_SIZE) {
                cpuInfo.remove(0);
            }

            if (ramInfo.size() > MAX_INFO_SIZE) {
                ramInfo.remove(0);
            }

        } catch (Exception e) {
            logger.error("[SystemInfo] Error while getting system info", e);
        }
    }

    @Override
    public WebSocketMessage processCommand(String command, String message, Object extra) {
        return null;
    }


    @Override
    protected Object refresh(String size) throws Exception {
        SystemInfoData data = new SystemInfoData();

        if (cpuInfo.size() > 0 && ramInfo.size() > 0) {
            data.cpuInfo = this.cpuInfo;
            data.ramInfo = this.ramInfo;
        }

        if (size == ModuleLayout.FULL_SCREEN) {
            data.hardwareInfo = hardwareInfo;
        }

        return data;
    }

    // ////////////
    // Class method
    // //////////

    /**
     * Getting data
     */
    public CpuInfo getCPUInfo() {

        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        Sensors sensors = systemInfo.getHardware().getSensors();

        CpuInfo info = new CpuInfo();

        info.cpuUsage = Math.ceil(processor.getSystemCpuLoad() * 100);

        try {
            info.fanSpeed = sensors.getFanSpeeds();
            info.temperature = sensors.getCpuTemperature();
            info.voltage = sensors.getCpuVoltage();
        } catch (Exception e) {
            logger.error("Couldn't read sensors", e);
        }

        return info;
    }

    public RamInfo getRamInfo() {
        RamInfo info = new RamInfo();


        GlobalMemory memory = systemInfo.getHardware().getMemory();


        info.maxRam = memory.getTotal();

        info.availableRam = memory.getAvailable();

        info.usedRam = info.maxRam - info.availableRam;

        info.percentageUsed = Math.ceil((info.usedRam / info.maxRam) * 100);

        return info;
    }


}
