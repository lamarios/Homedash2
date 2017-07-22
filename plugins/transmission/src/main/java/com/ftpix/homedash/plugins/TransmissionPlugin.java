package com.ftpix.homedash.plugins;

import com.ftpix.homedash.Utils.ByteUtils;
import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.notifications.Notifications;
import com.ftpix.homedash.plugins.models.TorrentObject;
import com.ftpix.homedash.plugins.models.TorrentSession;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ca.benow.transmission.AddTorrentParameters;
import ca.benow.transmission.TransmissionClient;
import ca.benow.transmission.model.TorrentStatus;
import ca.benow.transmission.model.TransmissionSession;


/**
 * Created by gz on 07-Jun-16.
 */
public class TransmissionPlugin extends Plugin {
    private TransmissionClient client;


    public static final String SETTING_URL = "url", SETTING_PORT = "port", SETTING_USERNAME = "username", SETTING_PASSWORD = "password", SETTING_AUTO_DELETE = "autoDelete", SETTING_AUTO_REMOVE_FILE = "autoRemoveFile";
    public static final String METHOD_ADDTORRENT = "addTorrent", METHOD_ALTSPEED = "altSpeed", METHOD_REMOVETORRENT = "removeTorrent", METHOD_REMOVETORRENT_DELETE = "removeTorrentDelete", METHOD_PAUSETORRENT = "pauseTorrent";

    private final String VOICE_THROTTLE = "torrent throttle", VOICE_FULL_SPEED = "torrent full speed";

    private boolean autoDelete = false, autoRemoveFile = false;

    @Override
    public String getId() {
        return "transmission";
    }

    @Override
    public String getDisplayName() {
        return "Transmission";
    }

    @Override
    public String getDescription() {
        return "Monitor and manage your torrent downloads";
    }

    @Override
    public String getExternalLink() {
        return "http://" + settings.get(SETTING_URL) + ":" + settings.get(SETTING_PORT);
    }

    @Override
    protected void init() {
        logger.info("Initiating Transmission plugin.");


        this.client = createClient(settings);

        autoDelete = settings.getOrDefault(SETTING_AUTO_DELETE, "0").equalsIgnoreCase("1");
        autoRemoveFile = settings.getOrDefault(SETTING_AUTO_REMOVE_FILE, "0").equalsIgnoreCase("1");

        logger.info("Transmission client ready !");
    }

    @Override
    public String[] getSizes() {
        return new String[]{"3x2", "2x1", "2x2", ModuleLayout.FULL_SCREEN, ModuleLayout.KIOSK};
    }

    @Override
    public int getBackgroundRefreshRate() {
        if (autoDelete) {
            return ONE_HOUR;
        } else {
            return NEVER;
        }
    }

    @Override
    public WebSocketMessage processCommand(String command, String message, Object extra) {
        logger.info("[Transmission] Received command [{}], message [{}]", command, message);

        WebSocketMessage response = new WebSocketMessage();

        if (command.equalsIgnoreCase(METHOD_ADDTORRENT)) {
            response = addTorrent(message);
        } else if (command.equalsIgnoreCase(METHOD_ALTSPEED)) {
            response = altSpeed(message.equalsIgnoreCase("true"));
        } else if (command.equalsIgnoreCase(METHOD_PAUSETORRENT)) {
            response = pauseTorrent(message);
        } else if (command.equalsIgnoreCase(METHOD_REMOVETORRENT)) {
            response = removeTorrent(message, false);
        } else if (command.equalsIgnoreCase(METHOD_REMOVETORRENT_DELETE)) {
            response = removeTorrent(message, true);
        } else {
            response.setCommand(WebSocketMessage.COMMAND_ERROR);
            response.setMessage("No matching method.");
        }
        return response;
    }

    @Override
    public void doInBackground() {
        if (autoDelete) {
            logger.info("Looking for torrents to delete");
            TorrentSession torrentSession = fullScreenRefresh();
            List<TorrentObject> torrents = torrentSession.torrents;


            //finding all torrents that have a status of finished
            Object[] ids = torrents.stream()
                    .filter(t -> {
                        return t.percentDone == 1
                                &&
                                (
                                        TorrentStatus.parseStatus(t.status, torrentSession.rpcVersion).equals(TorrentStatus.StatusField.finished)
                                                || TorrentStatus.parseStatus(t.status, torrentSession.rpcVersion).equals(TorrentStatus.StatusField.stopped)
                                );

                    })
                    .map(t -> {
                        Notifications.send("Deleting torrent", t.name);
                        return Integer.valueOf(t.id);
                    })
                    .collect(Collectors.toList()).toArray();

            try {
                logger.info("Deleting torrents {}", ids);
                client.removeTorrents(ids, autoRemoveFile);
            } catch (IOException e) {
                logger.error("Couldn't delete torrents", e);
            }
        }
    }

    @Override
    protected Object refresh(String size) throws Exception {
        try {
            if (size.equalsIgnoreCase(ModuleLayout.FULL_SCREEN)) {
                return fullScreenRefresh();
            } else {
                return getSessionStats();
            }
        } catch (Exception e) {
            logger.error("Error while refreshing transmission", e);
            return false;
        }
    }

    @Override
    public int getRefreshRate(String size) {
        return ONE_SECOND * 5;
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> errors = new Hashtable<>();

        String url = settings.get(SETTING_URL);

        if (!url.trim().equalsIgnoreCase("")) {
            try {
                int port = Integer.parseInt(settings.get(SETTING_PORT));

                TransmissionClient client = createClient(settings);

                try {
                    client.getSession();
                } catch (JSONException | IOException e) {
                    errors.put("Unreachable", "One of your settings is incorrect (url, port, username and/or pasword)");
                }
            } catch (NumberFormatException e) {
                errors.put("Port", "must be a number");
            }
        } else {
            errors.put("Host", "Host can't be empty");
        }

        return errors;
    }

    @Override
    public ModuleExposedData exposeData() {
        try {
            ModuleExposedData data = new ModuleExposedData();
            TorrentSession session = getSessionStats();
            data.addText(session.status.getTorrentCount() + " torrents");
            data.addText("DL: " + ByteUtils.humanReadableByteCount(session.status.getDownloadSpeed(), true) + "/s");
            data.addText("UL: " + ByteUtils.humanReadableByteCount(session.status.getUploadSpeed(), true) + "/s");
            return data;
        } catch (Exception e) {
            logger.error("Couldn't get transmission exposed data", e);
            return null;
        }
    }

    @Override
    public Map<String, String> exposeSettings() {
        Map<String, String> result = new Hashtable<>();
        result.put("Transmission URL", settings.get(SETTING_URL));
        return result;
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

    //////////////////////////////////////////////////////
    ////// plugin methods

    public TorrentSession fullScreenRefresh() {
        TorrentSession obj = new TorrentSession();

        try {
            obj = getSessionStats();

            TorrentStatus.TorrentField[] fields = new TorrentStatus.TorrentField[]{TorrentStatus.TorrentField.name, TorrentStatus.TorrentField.rateDownload, TorrentStatus.TorrentField.rateUpload, TorrentStatus.TorrentField.percentDone, TorrentStatus.TorrentField.id, TorrentStatus.TorrentField.status,
                    TorrentStatus.TorrentField.downloadedEver, TorrentStatus.TorrentField.uploadedEver, TorrentStatus.TorrentField.totalSize, TorrentStatus.TorrentField.seedRatioLimit};

            logger.info("" + obj.rpcVersion);
            obj.torrents = new ArrayList<TorrentObject>();
            for (TorrentStatus torrent : client.getAllTorrents(fields)) {
                TorrentObject t = new TorrentObject();
                t.mapTorrent(torrent, obj.rpcVersion);

                obj.torrents.add(t);
            }
        } catch (Exception e) {
            logger.error("error while getting torrents", e);
        }

        return obj;
    }


    private TransmissionClient createClient(Map<String, String> settings) {
        TransmissionClient client;
        if (settings.get(SETTING_USERNAME).equalsIgnoreCase("") || settings.get(SETTING_PASSWORD).equalsIgnoreCase("")) {
            logger.info("Connecting to [{}] No username and password.", settings.get(SETTING_URL) + ":" + settings.get(SETTING_PORT));
            client = new TransmissionClient(settings.get(SETTING_URL), Integer.parseInt(settings.get(SETTING_PORT)));
        } else {
            logger.info("Connecting to [{}] Using username [{}] and password.", settings.get(SETTING_URL) + ":" + settings.get(SETTING_PORT), settings.get(SETTING_USERNAME));
            client = new TransmissionClient(settings.get(SETTING_URL), Integer.parseInt(settings.get(SETTING_PORT)), settings.get(SETTING_USERNAME), settings.get(SETTING_PASSWORD));
        }

        return client;

    }

    private WebSocketMessage altSpeed(boolean altSpeed) {
        WebSocketMessage response = new WebSocketMessage();
        try {

            TransmissionSession.SessionPair pair = new TransmissionSession.SessionPair(TransmissionSession.SessionField.altSpeedEnabled, altSpeed);
            client.setSession(pair);

            response.setCommand(WebSocketMessage.COMMAND_SUCCESS);
            response.setMessage("Alternate speed set successfully !");

        } catch (Exception e) {
            response.setCommand(WebSocketMessage.COMMAND_ERROR);
            response.setMessage("Error while seting alternate speed.");
        }
        return response;
    }

    private WebSocketMessage addTorrent(String url) {
        WebSocketMessage response = new WebSocketMessage();
        try {
            AddTorrentParameters params = new AddTorrentParameters(url);
            client.addTorrent(params);
            response.setCommand(WebSocketMessage.COMMAND_SUCCESS);
            response.setMessage("Torrent added successfully !");

        } catch (Exception e) {
            response.setCommand(WebSocketMessage.COMMAND_ERROR);
            response.setMessage("Error while adding torrent.");
        }

        return response;

    }

    private WebSocketMessage pauseTorrent(String idArray) {
        WebSocketMessage response = new WebSocketMessage();
        try {
            int[] ids = gson.fromJson(idArray, int[].class);
            if (ids.length == 1) {

                TorrentStatus torrent = client.getTorrents(ids, TorrentStatus.TorrentField.status).get(0);

                if (torrent.getStatus() == TorrentStatus.StatusField.stopped) {
                    client.startTorrents(ids[0]);
                    response.setMessage("Torrent resumed successfully !");
                } else {
                    client.stopTorrents(ids[0]);
                    response.setMessage("Torrent paused successfully !");
                }
            } else {
                client.stopTorrents(ids);
            }

            response.setCommand(WebSocketMessage.COMMAND_SUCCESS);

        } catch (Exception e) {
            response.setCommand(WebSocketMessage.COMMAND_ERROR);
            response.setMessage("Error while seting alternate speed.");
        }
        return response;
    }

    private WebSocketMessage removeTorrent(String idArray, boolean delete) {

        WebSocketMessage response = new WebSocketMessage();
        try {
            Object[] ids = gson.fromJson(idArray, Object[].class);
            client.removeTorrents(ids, delete);
            response.setCommand(WebSocketMessage.COMMAND_SUCCESS);
            response.setMessage("Torrent removed successfully !");

        } catch (Exception e) {
            response.setCommand(WebSocketMessage.COMMAND_ERROR);
            response.setMessage("Error while seting alternate speed.");
        }
        return response;
    }

    private TorrentSession getSessionStats() throws JSONException, IOException {
        TorrentSession obj = new TorrentSession();

        Map<TransmissionSession.SessionField, Object> session = client.getSession();
        obj.status = client.getSessionStats();
        obj.rpcVersion = Integer.parseInt(session.get(TransmissionSession.SessionField.rpcVersion).toString());
        obj.alternateSpeeds = (Boolean) session.get(TransmissionSession.SessionField.altSpeedEnabled);

        return obj;
    }
}
