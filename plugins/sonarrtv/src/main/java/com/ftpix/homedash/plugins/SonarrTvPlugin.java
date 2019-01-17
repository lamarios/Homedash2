package com.ftpix.homedash.plugins;

import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.api.SonarrApi;
import com.ftpix.homedash.plugins.api.SonarrUnauthorizedException;
import com.ftpix.homedash.plugins.api.models.SonarrCalendar;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by gz on 06-Jun-16.
 */
public class SonarrTvPlugin extends Plugin {

    public static final String URL = "url", API_KEY = "apiKey";
    private static final int THUMB_SIZE = 500;
    private final String IMAGE_PATH = "images/";
    private String url, apiKey;
    private SonarrApi api;
    private final static String COMMAND_SEARCH = "search", COMMAND_QUALITIES = "qualities", COMMAND_FOLDERS = "folders", COMMAND_ADD_SHOW="add-show";

    @Override
    public String getId() {
        return "sonarrtv";
    }

    @Override
    public String getDisplayName() {
        return "Sonarr TV";
    }

    @Override
    public String getDescription() {
        return "View the upcomping tv show";
    }

    @Override
    public String getExternalLink() {
        return url;
    }

    @Override
    protected void init() {
        logger().info("Initiating Sonarr plugin.");
        url = settings.get(URL);

        apiKey = settings.get(API_KEY);

        api = new SonarrApi(url, apiKey);
    }

    @Override
    public String[] getSizes() {
        return new String[]{ModuleLayout.SIZE_2x2, "3x3", "4x4", "3x1", ModuleLayout.FULL_SCREEN, ModuleLayout.KIOSK};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return 0;
    }

    @Override
    public WebSocketMessage processCommand(String command, String message, Object extra) {
        WebSocketMessage response = new WebSocketMessage();
        response.setCommand(command);
        try {
            switch (command) {
                case COMMAND_SEARCH:
                    String results = api.searchSeries(message);
                    response.setMessage(results);
                    break;
                case COMMAND_QUALITIES:
                    response.setMessage(api.getQualities());
                    break;
                case COMMAND_FOLDERS:
                    response.setMessage(api.getFolders());
                    break;
                case COMMAND_ADD_SHOW:
                    if(api.addShow(message)){
                     response.setCommand(WebSocketMessage.COMMAND_SUCCESS);
                     response.setMessage("Show added successfully");
                    }else{
                        response.setCommand(WebSocketMessage.COMMAND_ERROR);
                        response.setMessage("Couldn't add show");
                    }
            }
        } catch (Exception e) {
            logger().error("Couldn't process command", e);
            response.setCommand(WebSocketMessage.COMMAND_ERROR);
            response.setMessage(e.getMessage());
        }

        return response;
    }

    @Override
    public void doInBackground() {
    }

    @Override
    protected Object refresh(String size) throws Exception {
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.MONTH, 1);
        List<SonarrCalendar> calendar = api.getCalendar(null, cal.getTime(), false);


        calendar.parallelStream().forEach((series) -> {
            downloadFanArt(series);
        });

        return calendar;
    }

    @Override
    public int getRefreshRate(String size) {
        if (size.equalsIgnoreCase(ModuleLayout.FULL_SCREEN)) {
            return Plugin.NEVER;
        } else {
            return ONE_HOUR;
        }
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> errors = new HashMap<>();

        logger().info("Initiating Sonarr plugin for settings validation.");
        String url = settings.get(URL);

        String apiKey = settings.get(API_KEY);

        SonarrApi api = new SonarrApi(url, apiKey);

        try {
            api.checkApi();
        } catch (IOException e) {
            errors.put("Unavailable", "Unable to reach Sonarr installation, make sure the address is correct");
        } catch (SonarrUnauthorizedException e) {
            errors.put("Unauthorized", "The API key is incorrect");
        }


        logger().info("Sonnarr Settings erorr size: {}", errors.size());
        return errors;
    }

    @Override
    public ModuleExposedData exposeData() {
        SonarrApi api = new SonarrApi(settings.get(URL), settings.get(API_KEY));

        try {
            Calendar cal = new GregorianCalendar();
            cal.add(Calendar.MONTH, 1);
            List<SonarrCalendar> calendar = api.getCalendar(null, cal.getTime());
            if (!calendar.isEmpty()) {
                SonarrCalendar series = calendar.get(0);
                downloadFanArt(series);

                ModuleExposedData data = new ModuleExposedData();
                data.addText(series.getAirDate());
                data.addText(series.getSeriesName());
                data.addText(series.getEpisodeName());
                data.addImage(series.getFanart());

                return data;

            }
        } catch (IOException | SonarrUnauthorizedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Map<String, String> exposeSettings() {
        Map<String, String> result = new Hashtable<>();
        result.put("Sonarr URL", settings.get(URL));
        return result;
    }

    @Override
    protected void onFirstClientConnect() {

    }

    @Override
    protected void onLastClientDisconnect() {

    }


    private Path getImagePath() throws IOException {
        Path resolve = getCacheFolder().resolve(IMAGE_PATH);
        if (!java.nio.file.Files.exists(resolve)) {
            java.nio.file.Files.createDirectories(resolve);
        }
        return resolve;
    }

    @Override
    protected Map<String, Object> getSettingsModel() {
        return null;
    }
    //////////////////////////
    /////plugin methods


    /**
     * Will download and resize pictures
     *
     * @param series
     */
    private void downloadFanArt(SonarrCalendar series) {
        File f = null;
        try {
            f = getImagePath().resolve(series.getSeriesId() + "-fanart.jpg").toFile();
        } catch (IOException e) {
            logger().error("Couldn't get fanart");
            return;
        }

        logger().info("Series: \n {}", series.toString());

        if (!f.exists()) {
            for (int i = 1; i <= 10; i++) {
                try {

                    String poster = series.getFanart();
                    logger().info("Download from: {}", poster);

                    FileUtils.copyURLToFile(new URL(poster), f);

                    // resizing the picture.
                    BufferedImage image = ImageIO.read(f);
                    BufferedImage resize = Scalr.resize(image, THUMB_SIZE);

                    ImageIO.write(resize, Files.getFileExtension(f.getName()), f);

                    break;
                } catch (Exception e) {
                    logger().info(". Path: {}", new File(".").getAbsolutePath());
                    logger().info("FULL_PNG Path: {} Writable ? {}", f.getAbsolutePath(), f.canWrite());
                    logger().info("f Path: {}", f.getAbsolutePath());
                    logger().error("Couldn't get poster for show [" + series.getSeriesId() + "]", e);
                }
            }
        }
        try {
            series.setFanart(getImagePath().resolve(series.getSeriesId() + "-fanart.jpg").toString());
        } catch (IOException e) {
            logger().error("Couldn't get fanart");
        }
    }
}
