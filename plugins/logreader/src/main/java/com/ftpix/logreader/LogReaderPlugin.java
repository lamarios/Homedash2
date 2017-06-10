package com.ftpix.logreader;

import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by gz on 6/10/17.
 */
public class LogReaderPlugin extends Plugin implements TailerListener {
    private final static String SETTINGS_PATH = "path", SETTINGS_LINES = "lines";

    private int maxLines = 200, linesSinceRefresh = 0;

    private Tailer tailer;
    private boolean tailerRunning = false;
    private Queue<String> lines = new ConcurrentLinkedQueue<>();

    @Override
    public String getId() {
        return "logreader";
    }

    @Override
    public String getDisplayName() {
        return "Log Reader";
    }

    @Override
    public String getDescription() {
        return "Follow the logs of a single file";
    }

    @Override
    public String getExternalLink() {
        return null;
    }

    @Override
    protected void init() {
        maxLines = Integer.parseInt(settings.getOrDefault(SETTINGS_LINES, "200"));
    }

    @Override
    public String[] getSizes() {
        return new String[]{ModuleLayout.SIZE_1x1, ModuleLayout.FULL_SCREEN};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return ONE_MINUTE * 10;
    }

    @Override
    protected WebSocketMessage processCommand(String command, String message, Object extra) {
        return null;
    }

    @Override
    public void doInBackground() {
        //getting the "maxLines" last lines of the files so that if the user visits the plugin when there is no activity, at least he/she can see something
        if (!tailerRunning) {
            try {
                ReversedLinesFileReader reversedLinesFileReader = new ReversedLinesFileReader(new File(settings.get(SETTINGS_PATH)));
                String line = null;
                List<String> linesTemp = new ArrayList<>();
                do {
                    line = reversedLinesFileReader.readLine();
                    linesTemp.add(0, line);
                } while (line != null && linesTemp.size() < maxLines);

                logger.info("lines {}", lines.size());
                logger.info("lines temp {}", linesTemp.size());
                lines.addAll(linesTemp);
                logger.info("fetched {} lines from {}", lines.size(), settings.get(SETTINGS_PATH));

            } catch (IOException e) {
                logger.info("Couldn't read file {}", settings.get(SETTINGS_PATH), e);
            }
        }
    }

    @Override
    protected Object refresh(String size) throws Exception {
        if (size.equalsIgnoreCase(ModuleLayout.FULL_SCREEN)) {
            return lines;
        } else {
            Map<String, String> data = new HashMap<>();
            data.put("path", new File(settings.get(SETTINGS_PATH)).getName());
            data.put("sinceRefresh", Integer.toString(linesSinceRefresh));
            linesSinceRefresh = 0;
            return data;
        }
    }

    @Override
    public int getRefreshRate(String size) {
        return ONE_SECOND;
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> errors = new HashMap<>();
        Path path = Paths.get(settings.get(SETTINGS_PATH));
        if (!Files.exists(path)) {
            errors.put("File", "The file given doesn't exist");
        } else {

            if (!Files.isDirectory(path)) {
                if (!Files.isReadable(path)) {
                    errors.put("Not readable", "The path given isn't readable");
                }
            } else {
                errors.put("Directory", "The path given is a directory");
            }
        }

        String maxLines = settings.get(SETTINGS_LINES);

        if (!StringUtils.isNumeric(maxLines)) {
            errors.put("Max lines", "has to be a number");
        }

        return errors;
    }

    @Override
    public ModuleExposedData exposeData() {
        ModuleExposedData data = new ModuleExposedData();

        return data;
    }

    @Override
    public Map<String, String> exposeSettings() {
        return settings;
    }

    @Override
    protected void onFirstClientConnect() {
        tailer = Tailer.create(new File(settings.get(SETTINGS_PATH)), this, 1000, true);
        tailerRunning = true;
    }

    @Override
    protected void onLastClientDisconnect() {
        logger.info("Stopping to tail {}", settings.get(SETTINGS_PATH));
        tailer.stop();
        tailerRunning = false;
    }

    @Override
    protected Map<String, Object> getSettingsModel() {
        return null;
    }

    //File tail
    @Override
    public void init(Tailer tailer) {

    }

    @Override
    public void fileNotFound() {

    }

    @Override
    public void fileRotated() {

    }

    @Override
    public void handle(String s) {
        lines.add(s);
        if (lines.size() > maxLines) {
            lines.remove();
        }
        linesSinceRefresh++;

    }

    @Override
    public void handle(Exception e) {

    }
}
