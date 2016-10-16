package com.ftpix.homedash.plugins.docker;

import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;

import java.util.Map;

public class DockerPlugin extends Plugin {
    public DockerPlugin() {
    }

    public DockerPlugin(Module module) {
        super(module);
    }

    @Override
    public String getId() {
        return "docker";
    }

    @Override
    public String getDisplayName() {
        return "Docker";
    }

    @Override
    public String getDescription() {
        return "Control your docker containers with this module";
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
        return new String[]{ModuleLayout.SIZE_1x1};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return NEVER;
    }

    @Override
    public void doInBackground() {
    }

    @Override
    public WebSocketMessage processCommand(String command, String message, Object extra) {
        return null;
    }

    @Override
    public Object refresh(String size) throws Exception {
        return NEVER;
    }

    @Override
    public int getRefreshRate(String size) {
        // TODO Auto-generated method stub
        return 0;
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

}
