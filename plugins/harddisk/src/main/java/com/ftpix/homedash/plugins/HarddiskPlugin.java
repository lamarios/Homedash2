package com.ftpix.homedash.plugins.harddisk;

import com.ftpix.homedash.Utils.ByteUtils;
import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;

import java.io.File;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by gz on 06-Jun-16.
 */
public class HarddiskPlugin extends Plugin{
    private final String SETTING_PATH = "path";



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

    }

    @Override
    public String[] getSizes() {
        return new String[]{"1x1", "2x1"};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return 0;
    }

    @Override
    public WebSocketMessage processCommand(String command, String message, Object extra) {
        return null;
    }

    @Override
    public void doInBackground() {

    }

    @Override
    protected Object refresh(String size) throws Exception {
        File root = new File(settings.get(SETTING_PATH));

        long usedSpace = root.getTotalSpace() - root.getFreeSpace();

        Map<String, String> diskSpace = new Hashtable<String, String>();


        Map<String, String> spaces = new Hashtable<>();
        //String[] space = new String[] { root.getTotalSpace(), true), humanReadableByteCount(root.getFreeSpace(), true), humanReadableByteCount(usedSpace, true) };
        spaces.put("path", root.getAbsolutePath());
        spaces.put("total", Long.toString(root.getTotalSpace()));
        spaces.put("free", Long.toString(root.getFreeSpace()));
        spaces.put("used", Long.toString(usedSpace));
        spaces.put("pretty", ByteUtils.humanReadableByteCount(usedSpace, root.getTotalSpace(), true));

        return spaces;
    }

    @Override
    public int getRefreshRate() {
        return ONE_MINUTE*2;
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> errors = new Hashtable<>();

        if(!new File(settings.get(SETTING_PATH)).exists()){
            errors.put("Path", "This mount point doesn't exist.");
        }

        return errors;
    }

    @Override
    public ModuleExposedData exposeData() {
        ModuleExposedData data = new ModuleExposedData();

        File root = new File(settings.get(SETTING_PATH));
        long usedSpace = root.getTotalSpace() - root.getFreeSpace();

        data.addText(root.getAbsolutePath());
        data.addText(ByteUtils.humanReadableByteCount(usedSpace, root.getTotalSpace(), true));
        return data;
    }

    @Override
    public Map<String, String> exposeSettings() {
        Map<String, String> result = new Hashtable<>();
        result.put("Path", settings.get(SETTING_PATH));
        return result;
    }




}
