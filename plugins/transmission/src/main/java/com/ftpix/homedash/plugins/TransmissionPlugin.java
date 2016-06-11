package com.ftpix.homedash.plugins;

import ca.benow.transmission.AddTorrentParameters;
import ca.benow.transmission.TransmissionClient;
import ca.benow.transmission.model.TorrentStatus;
import ca.benow.transmission.model.TransmissionSession;
import com.ftpix.homedash.Utils.ByteUtils;
import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.models.TorrentObject;
import com.ftpix.homedash.plugins.models.TorrentSession;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by gz on 07-Jun-16.
 */
public class TransmissionPlugin extends Plugin {
    private TransmissionClient client;


    public static final String URL = "url", PORT = "port", USERNAME = "username", PASSWORD = "password";
    public static final String METHOD_ADDTORRENT = "addTorrent", METHOD_ALTSPEED = "altSpeed", METHOD_REMOVETORRENT = "removeTorrent", METHOD_PAUSETORRENT = "pauseTorrent";

    private final String VOICE_THROTTLE = "torrent throttle", VOICE_FULL_SPEED = "torrent full speed";


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
        return "http://" + settings.get(URL) + ":" + settings.get(PORT);
    }

    @Override
    protected void init() {
        logger.info("Initiating Transmission plugin.");


        this.client = createClient(settings);

        logger.info("Transmission client ready !");
    }

    @Override
    public String[] getSizes() {
        return new String[]{"3x2", "2x1", "2x2", ModuleLayout.FULL_SCREEN};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return NEVER;
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
            response = pauseTorrent(Integer.parseInt(message));
        } else if (command.equalsIgnoreCase(METHOD_REMOVETORRENT)) {
            response = removeTorrent(Integer.parseInt(message));
        } else {
            response.setCommand(WebSocketMessage.COMMAND_ERROR);
            response.setMessage("No matching method.");
        }
        return response;
    }

    @Override
    public void doInBackground() {

    }

    @Override
    protected Object refresh(String size) throws Exception {
        try {
            if(size.equalsIgnoreCase(ModuleLayout.FULL_SCREEN)){
                return fullScreenRefresh();
            }else {
                return getSessionStats();
            }
        } catch (Exception e) {
           logger.error("Error while refreshing transmission", e);
            return false;
        }
    }

    @Override
    public int getRefreshRate() {
        return ONE_SECOND * 5;
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> errors = new Hashtable<>();

        String url = settings.get(URL);

        if (!url.trim().equalsIgnoreCase("")) {
            try {
                int port = Integer.parseInt(settings.get(PORT));

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
        result.put("Transmission URL", settings.get(URL));
        return result;
    }

    //////////////////////////////////////////////////////
    ////// plugin methods

    public TorrentSession fullScreenRefresh(){
        TorrentSession obj = new TorrentSession();

        try {
            obj = getSessionStats();

            TorrentStatus.TorrentField[] fields = new TorrentStatus.TorrentField[] { TorrentStatus.TorrentField.name, TorrentStatus.TorrentField.rateDownload, TorrentStatus.TorrentField.rateUpload, TorrentStatus.TorrentField.percentDone, TorrentStatus.TorrentField.id, TorrentStatus.TorrentField.status,
                    TorrentStatus.TorrentField.downloadedEver, TorrentStatus.TorrentField.uploadedEver, TorrentStatus.TorrentField.totalSize };

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
        if (settings.get(USERNAME).equalsIgnoreCase("") || settings.get(PASSWORD).equalsIgnoreCase("")) {
            logger.info("Connecting to [{}] No username and password.", settings.get(URL) + ":" + settings.get(PORT));
            client = new TransmissionClient(settings.get(URL), Integer.parseInt(settings.get(PORT)));
        } else {
            logger.info("Connecting to [{}] Using username [{}] and password.", settings.get(URL) + ":" + settings.get(PORT), settings.get(USERNAME));
            client = new TransmissionClient(settings.get(URL), Integer.parseInt(settings.get(PORT)), settings.get(USERNAME), settings.get(PASSWORD));
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

    private WebSocketMessage pauseTorrent(int id) {
        WebSocketMessage response = new WebSocketMessage();
        try {
            int[] ids = {id};
            TorrentStatus torrent = client.getTorrents(ids, TorrentStatus.TorrentField.status).get(0);

            if (torrent.getStatus() == TorrentStatus.StatusField.stopped) {
                client.startTorrents(id);
                response.setMessage("Torrent resumed successfully !");
            } else {
                client.stopTorrents(id);
                response.setMessage("Torrent paused successfully !");
            }

            response.setCommand(WebSocketMessage.COMMAND_SUCCESS);

        } catch (Exception e) {
            response.setCommand(WebSocketMessage.COMMAND_ERROR);
            response.setMessage("Error while seting alternate speed.");
        }
        return response;
    }

    private WebSocketMessage removeTorrent(int id) {

        WebSocketMessage response = new WebSocketMessage();
        try {

            Object[] ids = {id};
            client.removeTorrents(ids, false);
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
