package com.ftpix.homedash.plugins;

import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleData;

/**
 * Created by gz on 12-Jun-16.
 */
public interface PluginListener {
    void saveModuleData(ModuleData data);
    void removeModuleData(ModuleData data);
}
