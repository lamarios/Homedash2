package com.ftpix.homedash.jobs;

import com.ftpix.homedash.app.PluginModuleMaintainer;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.plugins.Plugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class BackgroundRefresh implements Job {
	private Logger logger = LogManager.getLogger();

	private static long TIME = 0;

	
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		
		logger.info("Background task dtarting Time:{}", TIME);

		try {
			PluginModuleMaintainer.getAllPluginInstances().stream()
					.filter(p -> p.getBackgroundRefreshRate() > Plugin.NEVER && TIME % p.getBackgroundRefreshRate() == 0 && p.getModule().getRemote() == Module.LOCAL).forEach((plugin) -> {
						logger.info("Background task: plugin:[{}] module:[{}]", plugin.getId(), plugin.getModule().getId());
						plugin.doInBackground();
					});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		TIME++;

	}

}
