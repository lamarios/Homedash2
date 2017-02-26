package com.ftpix.homedash.plugins;

import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.api.SonarrApi;
import com.ftpix.homedash.plugins.api.SonarrUnauthorizedException;
import com.ftpix.homedash.plugins.api.models.SonarrCalendar;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Created by gz on 06-Jun-16.
 */
public class SonarrTvPlugin extends Plugin {

    public static final String URL = "url", API_KEY = "apiKey";
    private final String IMAGE_PATH = "images/";
    private String url, apiKey;
    private SonarrApi api;


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
        logger.info("Initiating Sonarr plugin.");
        url = settings.get(URL);

        apiKey = settings.get(API_KEY);

        api = new SonarrApi(url, apiKey);

        File f = new File(getImagePath());
        if (!f.exists()) {
            f.mkdirs();
        }
        f.setWritable(true);
        f.deleteOnExit();
    }

    @Override
    public String[] getSizes() {
        return new String[]{ModuleLayout.SIZE_2x2, "3x3", "4x4", "3x1"};
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
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.MONTH, 1);
        List<SonarrCalendar> calendar = api.getCalendar(null, cal.getTime(), false);


        calendar.forEach((series) -> {
            downloadFanArt(series);
        });

        return calendar;
    }

    @Override
    public int getRefreshRate(String size) {
        return ONE_HOUR;
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> errors = new HashMap<>();

        logger.info("Initiating Sonarr plugin for settings validation.");
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


        logger.info("Sonnarr Settings erorr size: {}", errors.size());
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


    private String getImagePath() {
        return getCacheFolder() + IMAGE_PATH;
    }

    @Override
    protected Map<String, Object> getSettingsModel() {
        return null;
    }
    //////////////////////////
    /////plugin methods

    private void downloadFanArt(SonarrCalendar series) {
        File f = new File(getImagePath() + series.getSeriesId() + "-fanart.jpg");

        logger.info("Series: \n {}", series.toString());

        if (!f.exists()) {
            for (int i = 1; i <= 10; i++) {
                try {

                    String poster = series.getFanart();
                    logger.info("Download from: {}", poster);

                    FileUtils.copyURLToFile(new URL(poster), f);

                    break;
                } catch (Exception e) {
                    logger.info(". Path: {}", new File(".").getAbsolutePath());
                    logger.info("FULL_PNG Path: {} Writable ? {}", f.getAbsolutePath(), f.canWrite());
                    logger.info("f Path: {}", f.getAbsolutePath());
                    logger.error("Couldn't get poster for show [" + series.getSeriesId() + "]", e);
                }
            }
        }
        series.setFanart(getImagePath() + series.getSeriesId() + "-fanart.jpg");
    }
}
