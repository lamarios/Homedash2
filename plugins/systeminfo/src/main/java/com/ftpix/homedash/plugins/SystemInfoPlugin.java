package com.ftpix.homedash.plugins;

import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.models.CpuInfo;
import com.ftpix.homedash.plugins.models.HardwareInfo;
import com.ftpix.homedash.plugins.models.OsInfo;
import com.ftpix.homedash.plugins.models.Process;
import com.ftpix.homedash.plugins.models.RamInfo;
import com.ftpix.homedash.plugins.models.SystemInfoData;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.Sensors;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

public class SystemInfoPlugin extends Plugin {

    private static final int PROCESS_LIST_SIZE = 50;
    private List<CpuInfo> cpuInfo = new ArrayList<CpuInfo>();
    private List<RamInfo> ramInfo = new ArrayList<RamInfo>();
    private HardwareInfo hardwareInfo = new HardwareInfo();
    private OsInfo osInfo = new OsInfo();
    private Map<Integer, Process> processes = new HashMap<>();

    private final int MAX_INFO_SIZE = 100, WARNING_THRESHOLD = 90;
    private final String SETTING_NOTIFICATIONS = "notifications", COMMAND_SORT = "sort";
    private final DecimalFormat nf = new DecimalFormat("#,###,###,##0.00");
    private final SystemInfo systemInfo = new SystemInfo();

    private long processLastCheck = System.currentTimeMillis();

    private Sort sorting = Sort.CPU;
    private boolean sortAlternate = false;

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
        hardwareInfo.vendor = processor.getVendor();

        OperatingSystem os = systemInfo.getOperatingSystem();
        osInfo.family = os.getFamily();
        osInfo.manufacturer = os.getManufacturer();
        osInfo.version = os.getVersion().getVersion();
        osInfo.build = os.getVersion().getBuildNumber();
        osInfo.codename = os.getVersion().getCodeName();
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

            mapProcesses();

        } catch (Exception e) {
            logger.error("[SystemInfo] Error while getting system info", e);
        }
    }

    @Override
    public WebSocketMessage processCommand(String command, String message, Object extra) {
        WebSocketMessage webSocketMessage = new WebSocketMessage();
        switch (command) {
            case COMMAND_SORT:
                Sort sort = Sort.valueOf(message);

                if (sort == this.sorting) {
                    sortAlternate = !sortAlternate;
                } else {
                    sortAlternate = false;
                }

                sorting = Sort.valueOf(message);
                webSocketMessage.setCommand(command);
                webSocketMessage.setMessage(message);
                break;
        }

        return new WebSocketMessage();
    }


    @Override
    protected Object refresh(String size) throws Exception {
        SystemInfoData data = new SystemInfoData();

        if (cpuInfo.size() > 0 && ramInfo.size() > 0) {
            data.cpuInfo = this.cpuInfo;
            data.ramInfo = this.ramInfo;
        }

        if (size.equalsIgnoreCase(ModuleLayout.FULL_SCREEN)) {
            data.hardwareInfo = hardwareInfo;

            data.osInfo = osInfo;

            Stream<Process> processStream = processes.values().stream();

            if (sortAlternate) {
                processStream = processStream.sorted(sorting.getComparator().reversed());
            } else {
                processStream = processStream.sorted(sorting.getComparator());
            }

            osInfo.processes = processStream
                    //.limit(PROCESS_LIST_SIZE)
                    .collect(Collectors.toList());


            System.out.println(processes.values().stream().mapToDouble(p -> p.cpuUsage).sum() + "%");
        }

        return data;
    }

    // ////////////
    // Class method
    // //////////

    /**
     * Map from OSHI processes and calculate CPU time;
     */
    public void mapProcesses() {

        Function<OSProcess, Process> mapProcess = (osp) -> {
            Process p = new Process();
            p.memory = osp.getResidentSetSize();
            p.name = osp.getName();
            p.pid = osp.getProcessID();
            p.cpuTime = osp.getKernelTime() + osp.getUserTime();

            return p;
        };

        final long current = System.currentTimeMillis();
        final long timeDiff = current - processLastCheck;
        processLastCheck = current;

        Consumer<Process> calculateCpuUsage = p -> {

            if (!processes.containsKey(p.pid)) {
                //It's the first time we're seeing this process, can't calculate a we need historical data.
                processes.put(p.pid, p);
            } else {
                //We calculate the CPU usage by checking how much cpu time it took within the last x seconds
                Process old = processes.get(p.pid);
                long diff = p.cpuTime - old.cpuTime;


                p.cpuUsage = ((double) diff / (double) timeDiff) * 100;
                processes.put(p.pid, p);
            }

        };

        OSProcess[] running = systemInfo.getOperatingSystem().getProcesses(Integer.MAX_VALUE, OperatingSystem.ProcessSort.CPU);


        Stream.of(running)
                .map(mapProcess)
                .forEach(calculateCpuUsage);

        Set<Integer> idList = Stream.of(running)
                .mapToInt(OSProcess::getProcessID)
                .boxed()
                .collect(Collectors.toSet());

        //cleaning list of process that no longer exist.

        new HashSet<Integer>(processes.keySet()).stream()
                .filter(id -> !idList.contains(id))
                .forEach(id -> {
                    processes.remove(id);
                });

    }


    /**
     * Getting data
     */
    public CpuInfo getCPUInfo() {

        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        Sensors sensors = systemInfo.getHardware().getSensors();

        CpuInfo info = new CpuInfo();

        info.cpuUsage = Math.ceil(processor.getSystemCpuLoad() * 100);


        hardwareInfo.uptime = processor.getSystemUptime();

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

    private enum Sort {
        CPU((p1, p2) -> Double.compare(p2.cpuUsage, p1.cpuUsage)),
        RAM((p1, p2) -> Long.compare(p2.memory, p1.memory)),
        NAME((p1, p2) -> p1.name.toLowerCase().compareTo(p2.name.toLowerCase())),
        PID((p1, p2) -> Integer.compare(p1.pid, p2.pid));

        private Comparator<Process> comparator;

        Sort(Comparator<Process> comparator) {
            this.comparator = comparator;

        }

        public Comparator<Process> getComparator() {
            return comparator;
        }
    }


}
