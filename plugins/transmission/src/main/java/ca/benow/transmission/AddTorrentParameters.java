package ca.benow.transmission;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AddTorrentParameters extends TorrentParameters {

    String torrentFileNameOrURL;
    InputStream metaInfo;
    boolean paused = false;

    public AddTorrentParameters(String torrentFileNameOrURL) {
        super();
        this.torrentFileNameOrURL = torrentFileNameOrURL;
    }

    /**
     * @param torrentStream inputstream to torrent contents (will be encoded)
     */
    public AddTorrentParameters(InputStream torrentStream) {
        super();
        this.metaInfo = torrentStream;
    }

    /**
     * @param paused if true, don't start the torrent
     * @return this
     */
    public AddTorrentParameters setPaused(boolean paused) {
        this.paused = paused;
        return this;
    }

    public JSONObject toRequestObject() throws IOException, JSONException {
        JSONObject obj = new JSONObject();
        if (location != null)
            obj.put("download-dir", location);
        if (torrentFileNameOrURL == null && metaInfo == null)
            throw new NullPointerException("A torrentFileNameOrURL or metaInfo parameter is required");
        if (torrentFileNameOrURL != null)
            obj.put("filename", torrentFileNameOrURL);
        if (metaInfo != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Base64.encode(metaInfo, out);
            String encoded = new String(out.toByteArray());
            obj.put("metainfo", encoded);
        }
        obj.put("paused", paused);
        if (peerLimit != null)
            obj.put("peer-limit", peerLimit);
        obj.put("bandwith-priority", bandwidthPriority);
        if (filesWanted != null && !filesWanted.isEmpty())
            obj.put("files-wanted", new JSONArray(filesWanted));
        if (filesUnwanted != null && !filesUnwanted.isEmpty())
            obj.put("files-unwanted", new JSONArray(filesUnwanted));
        if (priorityHigh != null && !priorityHigh.isEmpty())
            obj.put("priority-high", new JSONArray(priorityHigh));
        if (priorityLow != null && !priorityLow.isEmpty())
            obj.put("priority-low", new JSONArray(priorityLow));
        if (priorityNormal != null && !priorityNormal.isEmpty())
            obj.put("priority-normal", new JSONArray(priorityNormal));
        return obj;
    }

}
