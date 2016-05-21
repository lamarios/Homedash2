package com.ftpix.homedash.plugins.systeminfo;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.plugins.systeminfo.models.CpuInfo;
import com.ftpix.homedash.plugins.systeminfo.models.RamInfo;
import com.ftpix.homedash.plugins.systeminfo.models.SystemInfoData;

public class SystemInfoPlugin extends Plugin {

	private List<CpuInfo> cpuInfo = new ArrayList<CpuInfo>();
	private List<RamInfo> ramInfo = new ArrayList<RamInfo>();
	private final int MAX_INFO_SIZE = 100, WARNING_THRESHOLD = 90;
	private final String SETTING_NOTIFICATIONS = "notifications";
	private final DecimalFormat nf = new DecimalFormat("#,###,###,##0.00");

	private final Sigar sigar = new Sigar();

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
	public String[] getSizes() {
		return new String[] { ModuleLayout.SIZE_2x1, ModuleLayout.SIZE_1x1 };
	}

	@Override
	public int getBackgroundRefreshRate() {
		return ONE_SECOND * 3;
	}
	
	@Override
	public int getRefreshRate() {
		return ONE_SECOND * 3;
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
	public Object processCommand(String command, String message, Object extra) {
		return null;
	}
	

	@Override
	protected Object refresh(String size) throws Exception {
		SystemInfoData data = new SystemInfoData();

		if (cpuInfo.size() > 0 && ramInfo.size() > 0) {
			data.cpuInfo = this.cpuInfo;
			data.ramInfo = this.ramInfo;
		}

		return data;
	}

	// ////////////
	// Class method
	// //////////

	/**
	 * Getting data
	 */
	public CpuInfo getCPUInfo() throws SigarException {

		CpuInfo info = new CpuInfo();
		info.cpuUsage = Math.ceil(sigar.getCpuPerc().getCombined() * 100);
		return info;
	}

	public RamInfo getRamInfo() throws SigarException {
		RamInfo info = new RamInfo();

		Mem mem = sigar.getMem();

		info.maxRam = mem.getRam() * 1024 * 1024;

		info.availableRam = mem.getFree() ;

		info.usedRam = mem.getUsed() ;

		info.percentageUsed = Math.ceil((info.usedRam / info.maxRam) * 100);

		return info;
	}


	
	

}
