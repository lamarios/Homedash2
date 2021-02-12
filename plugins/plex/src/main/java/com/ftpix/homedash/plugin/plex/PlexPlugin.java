package com.ftpix.homedash.plugin.plex;

import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugin.plex.api.ApiType;
import com.ftpix.homedash.plugin.plex.api.MediaServerApi;
import com.ftpix.homedash.plugin.plex.model.NowPlaying;
import com.ftpix.homedash.plugins.Plugin;
import com.google.common.io.Files;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlexPlugin extends Plugin {

    private static final String IMAGE_PATH = "images/";
    private static final int THUMB_SIZE = 500;

    private MediaServerApi api;
    private ApiType type;

    @Override
    public String getId() {
        return "plex";
    }

    @Override
    public String getDisplayName() {
        return "Media server (Plex / Jellyfin)";
    }

    @Override
    public String getDescription() {
        return "Display what is currently playing on a Plex or Jellyfin server";
    }

    @Override
    public String getExternalLink() {
        return null;
    }

    @Override
    protected void init() {
        type = ApiType.valueOf(settings.get("type"));
        api = MediaServerApi.createFromSettings(settings);
    }


    @Override
    public String[] getSizes() {
        return new String[]{"1x1", "2x1", "3x2", "3x3", "4x4", ModuleLayout.KIOSK};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return 0;
    }

    @Override
    protected WebSocketMessage processCommand(String command, String message, Object extra) {
        return null;
    }

    @Override
    public void doInBackground() {

    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    protected Object refresh(String size) throws Exception {
        List<NowPlaying> nowPlaying = api.getNowPlaying();

        nowPlaying.forEach(np -> np.setImage(downloadPicture(np.getImage())));

        // download pictures
        Map<String, Object> data = new HashMap<>();
        data.put("videos", nowPlaying);
        data.put("type", type);
        return data;
    }

    @Override
    public int getRefreshRate(String size) {
        return ONE_SECOND * 5;
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        MediaServerApi api = MediaServerApi.createFromSettings(settings);
        return api.validateSettings(settings);
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
    protected void onFirstClientConnect() {

    }

    @Override
    protected void onLastClientDisconnect() {

    }

    @Override
    protected Map<String, Object> getSettingsModel() {
        return null;
    }

    //PLUGIN METHODS

    private Path getImagePath() throws IOException {

        Path resolve = getCacheFolder().resolve(IMAGE_PATH);
        if (!java.nio.file.Files.exists(resolve)) {
            java.nio.file.Files.createDirectories(resolve);
        }
        return resolve;
    }

    private String downloadPicture(String url) {
        try {
            Path filePath = getImagePath().resolve(DigestUtils.md5Hex(url) + ".jpg");


            if (!java.nio.file.Files.exists(filePath)) {


                logger().info("Download from: {}", url);

                FileUtils.copyURLToFile(new URL(url), filePath.toFile());

                // resizing the picture.
                BufferedImage image = ImageIO.read(filePath.toFile());
                BufferedImage resize = Scalr.resize(image, THUMB_SIZE);

                ImageIO.write(resize, Files.getFileExtension(filePath.toFile().getName()), filePath.toFile());


            }
            return "/" + filePath.toString().replace("/tmp/", "");
        } catch (Exception e) {
            logger().error("Couldn't get poster from path [{}]", url, e);
            return "";
        }


    }
}
