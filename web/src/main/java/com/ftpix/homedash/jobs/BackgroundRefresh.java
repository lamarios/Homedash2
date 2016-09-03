package com.ftpix.homedash.jobs;

import com.ftpix.homedash.app.PluginModuleMaintainer;
import com.ftpix.homedash.models.ModuleLocation;
import com.ftpix.homedash.plugins.Plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@DisallowConcurrentExecution
public class BackgroundRefresh implements Job {
    private Logger logger = LogManager.getLogger();

    private static long TIME = 0;


    @Override
    public void execute(JobExecutionContext arg0) throws JobExecutionException {

        logger.debug("Background task starting Time:{}", TIME);

        long time = TIME++;

        try {
            PluginModuleMaintainer.getInstance().getAllPluginInstances().stream()
                    .filter(p -> p.getBackgroundRefreshRate() > Plugin.NEVER && time % p.getBackgroundRefreshRate() == 0 && p.getModule().getLocation() == ModuleLocation.LOCAL).forEach((plugin) -> {
                try {
                    logger.info("Background task: plugin:[{}] module:[{}]", plugin.getId(), plugin.getModule().getId());
                    plugin.doInBackground();
                }catch(Exception e){
                    logger.info("Error during background refresh", e);
                }
            });
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }



    }


    /**
     * Resets the background refresh timer
     */
    public static void resetTimer(){
        TIME = 0;
    }

}
