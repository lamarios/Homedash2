package ca.benow.transmission;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.benow.transmission.model.AddedTorrentInfo;
import ca.benow.transmission.model.SessionStatus;
import ca.benow.transmission.model.TorrentStatus;
import ca.benow.transmission.model.TorrentStatus.TorrentField;
import ca.benow.transmission.model.TransmissionSession;
import ca.benow.transmission.model.TransmissionSession.SessionField;
import ca.benow.transmission.model.TransmissionSession.SessionPair;

/**
 * The main class for interacting with transmission. Create an instance with
 * appropriate parameters and call the methods. If possible, re-use the instance
 * to avoid slight network overhead.
 * <p/>
 * Here's an example.
 *
 * <pre>
 * // initialize log4j
 * DOMConfigurator.configure(&quot;etc/logging.xml&quot;);
 *
 * TransmissionClient client = new TransmissionClient(new URL(
 * 		&quot;http://transmission:transmission@localhost:9091/transmission/rpc&quot;));
 * List&lt;TorrentStatus&gt; torrents = client.getAllTorrents();
 * for (TorrentStatus curr : torrents)
 * 	System.out.println(curr);
 * </pre>
 *
 * By tuning the log4j config, it is possible to see the JSON RPC request and
 * response text, which might be useful for debugging.
 *
 * @author andy
 */
public class TransmissionClient {
    private Logger logger = LogManager.getLogger();

    private static final String ID_RECENTLY_ACTIVE = "recently-active";
    private static final String SESSION_HEADER = "X-Transmission-Session-Id";
    private static final int DEFAULT_PORT = 9091;

    private final URL url;
    private String user;
    private String pass;
    private String userCrypt;
    private String sessionId;
    private static int tag = 0;
    private int rpcVersion = 0;

    /**
     * Creates a new client that connects to a given url. URL should be something
     * similar to
     *
     * <pre>
     * http://transmission:transmission@localhost:9091/transmission/rpc
     * </pre>
     */
    public TransmissionClient(URL url) {
        this.url = url;
        if (url.getUserInfo() != null) {
            String uinfo = url.getUserInfo();
            if (uinfo != null)
                userCrypt = Base64.encode(uinfo);
        }
        // set rpc version
        try {
            Map<SessionField, Object> session = getSession();
            rpcVersion = (Integer) session.get(SessionField.rpcVersion);
        } catch (Exception e) {
            e.printStackTrace();
            rpcVersion = 0;
        }
    }

    /**
     * Creates a new client that connects to a given host on a given port with the
     * given user and password.
     *
     * @param host host to connect to
     * @param port port to connect on
     * @param user user to connect as
     * @param pass password for user
     */
    public TransmissionClient(String host, int port, String user, String pass) {
        this(createURL(host, port, user, pass));
    }

    /**
     * Creates a new client that connects to a given host on the default port
     * (9091) with the given user and pass
     *
     * @param host host to connect to
     * @param user user to connect as
     * @param pass password for user
     */
    public TransmissionClient(String host, String user, String pass) {
        this(createURL(host, DEFAULT_PORT, user, pass));
    }

    /**
     * Create a new client that connects to a given host on the default port
     * (9091) with the default user/pass (transmission/transmission)
     *
     * @param host host to connect to
     */
    public TransmissionClient(String host) {
        this(host, DEFAULT_PORT);
    }

    /**
     * Create a new client that connects to given host and port with default
     * user/pass (transmission/transmission)
     *
     * @param host host to connect to
     * @param port port to connect on
     */
    public TransmissionClient(String host, int port) {
        this(host, port, "transmission", "transmission");
    }

    /**
     * Creates a new client that connects to the local transmission using default
     * parameters
     */
    public TransmissionClient() {
        this(createURL("localhost", 9091, "transmission", "transmission"));
    }

    private static URL createURL(String host, int port, String user, String pass) {
        try {
            return new URL("http://" + (user == null ? "" : user + ":" + pass + "@") + host + ":" + port
                    + "/transmission/rpc");
        } catch (MalformedURLException e) {
            throw new RuntimeException("The impossible happened, again.", e);
        }
    }

    /**
     * Send a command to Transmission.
     *
     * @param name name of command, which forms the 'method' in request
     * @param args arguments which for body of 'arguments' in request
     * @return json object containing result of 'arguments' in response, if any
     * @throws IOException           on problem communicating
     * @throws TransmissionException on Transmission problem when performing the command
     */
    public JSONObject sendCommand(String name, JSONObject args) throws IOException, JSONException {
        HttpURLConnection hconn = (HttpURLConnection) url.openConnection();
        hconn.setRequestMethod("POST");
        hconn.setDoOutput(true);
        if (userCrypt != null)
            hconn.setRequestProperty("Authorization", "Basic " + userCrypt);
        if (sessionId != null)
            hconn.setRequestProperty(SESSION_HEADER, sessionId);

        JSONObject command = new JSONObject();
        command.put("method", name);
        command.put("arguments", args);
        command.put("tag", "" + tag++);

        String json = command.toString(2);
        OutputStream out = hconn.getOutputStream();
        out.write((json + "\r\n\r\n").getBytes());
        out.flush();
        out.close();

        BufferedReader in;
        try {
            in = new BufferedReader(new InputStreamReader(hconn.getInputStream()));
            if (logger.isDebugEnabled())
                logger.debug("Wrote:\n" + json);
        } catch (IOException e) {
            if (hconn.getResponseCode() == 409) {
                String sessId = hconn.getHeaderField(SESSION_HEADER);
                if (sessId != null) {
                    logger.debug("Reconnecting with new session id");
                    this.sessionId = sessId;
                    return sendCommand(name, args);
                }
            }
            throw e;
        }
        String msg = "";
        String line = in.readLine();
        while (line != null) {
            msg += line + "\n";
            line = in.readLine();
        }
        JSONObject result;
        try {
            JSONTokener toker = new JSONTokener(msg);
            result = new JSONObject(toker);
        } catch (JSONException e) {
            logger.error("Error parsing json: " + msg);
            throw e;
        }

        if (logger.isDebugEnabled())
            logger.debug("Read:\n" + result.toString(2));

        String resultStr = result.getString("result");
        if (!resultStr.equals("success"))
            throw new TransmissionException(resultStr, command.toString(2), msg);

        JSONObject resultArgs = null;
        if (result.has("arguments"))
            resultArgs = result.getJSONObject("arguments");
        return resultArgs;
    }

    /**
     * Get status of torrents
     *
     * @param ids             optional ids of torrents to fetch status for, if not given, status for
     *                        all torrents will be fetched
     * @param requestedFields information fields to fetch, if not given, only the id and name fields
     *                        are fetched
     * @return status for requested torrents
     */
    public List<TorrentStatus> getTorrents(int[] ids, TorrentStatus.TorrentField... requestedFields) throws IOException, JSONException {
        JSONObject args = new JSONObject();
        if (ids != null && ids.length > 0) {
            JSONArray idAry = new JSONArray();
            for (int i = 0; i < ids.length; i++)
                idAry.put(ids[i]);
            args.put("ids", idAry);
        }
        if (requestedFields == null)
            requestedFields = TorrentStatus.defaultFields;
        else {
            if (requestedFields.length > 0) {
                boolean hasAll = false;
                for (int i = 0; i < requestedFields.length; i++) {
                    if (requestedFields[i].equals(TorrentField.all))
                        hasAll = true;
                }
                if (hasAll) {
                    requestedFields = new TorrentField[TorrentField.values().length - 1];
                    for (int i = 0; i < requestedFields.length; i++)
                        requestedFields[i] = TorrentField.values()[i + 1];
                }
            }
        }
        JSONArray fields = new JSONArray();
        for (int i = 0; i < requestedFields.length; i++)
            fields.put(TorrentStatus.fieldNameByFieldPos[requestedFields[i].ordinal()]);
        args.put("fields", fields);

        List<TorrentStatus> torrents = new ArrayList<TorrentStatus>();
        JSONObject result = sendCommand("torrent-get", args);
        JSONArray torAry = result.getJSONArray("torrents");
        for (int i = 0; i < torAry.length(); i++) {
            torrents.add(new TorrentStatus(torAry.getJSONObject(i), rpcVersion));
            //      torrents.add(new TorrentStatus(torAry.getJSONObject(i)));
        }

        return torrents;
    }

    public List<TorrentStatus> getAllTorrents(TorrentField[] torrentFields) throws IOException, JSONException {
        return getTorrents(null, torrentFields);
    }

    public List<TorrentStatus> getAllTorrents() throws IOException, JSONException {
        return getTorrents(new int[]{});
    }

    /**
     * Adds a new torrent
     *
     * @param params parameters for torrent addition
     * @return info about the added torrent
     */
    public AddedTorrentInfo addTorrent(AddTorrentParameters params) throws IOException, JSONException {
        JSONObject obj = params.toRequestObject();
        JSONObject result = sendCommand("torrent-add", obj);
        return new AddedTorrentInfo(result.getJSONObject("torrent-added"));
    }

    /**
     * Start given torrents
     *
     * @param ids numerical ids, string hashes or the ID_RECENTLY_ADDED constant
     */
    public void startTorrents(Object... ids) throws IOException, JSONException {
        if (ids == null)
            throw new NullPointerException("At least one id is required");
        JSONObject obj = new JSONObject();
        if (ids.length == 1)
            obj.put("ids", ids[0]);
        else {
            JSONArray ary = new JSONArray();
            for (int i = 0; i < ids.length; i++)
                ary.put(ids[i]);
            obj.put("ids", ary);
        }
        sendCommand("torrent-start", obj);
    }

    /**
     * Stop given torrents
     *
     * @param ids numerical ids, string hashes or the ID_RECENTLY_ADDED constant
     */
    public void stopTorrents(Object... ids) throws IOException, JSONException {
        if (ids == null)
            throw new NullPointerException("At least one id is required");
        JSONObject obj = new JSONObject();
        if (ids.length == 1)
            obj.put("ids", ids[0]);
        else {
            JSONArray ary = new JSONArray();
            for (int i = 0; i < ids.length; i++)
                ary.put(ids[i]);
            obj.put("ids", ary);
        }
        sendCommand("torrent-stop", obj);
    }

    /**
     * Verify given torrents
     *
     * @param ids numerical ids, string hashes or the ID_RECENTLY_ADDED constant
     */
    public void verifyTorrents(Object... ids) throws IOException, JSONException {
        if (ids == null)
            throw new NullPointerException("At least one id is required");
        JSONObject obj = new JSONObject();
        if (ids.length == 1)
            obj.put("ids", ids[0]);
        else {
            JSONArray ary = new JSONArray();
            for (int i = 0; i < ids.length; i++)
                ary.put(ids[i]);
            obj.put("ids", ary);
        }
        sendCommand("torrent-verify", obj);
    }

    /**
     * Reannounce (fetch new peers) for given torrents
     *
     * @param ids numerical ids, string hashes or the ID_RECENTLY_ADDED constant
     */
    public void reannounceTorrents(Object... ids) throws IOException, JSONException {
        if (ids == null)
            throw new NullPointerException("At least one id is required");
        JSONObject obj = new JSONObject();
        if (ids.length == 1)
            obj.put("ids", ids[0]);
        else {
            JSONArray ary = new JSONArray();
            for (int i = 0; i < ids.length; i++)
                ary.put(ids[i]);
            obj.put("ids", ary);
        }
        sendCommand("torrent-reannounce", obj);
    }

    /**
     * Sets properties of selected torrents
     *
     * @param params parameters for torrents
     */
    public void setTorrents(SetTorrentParameters params) throws IOException, JSONException {
        JSONObject obj = params.toRequestObject();
        sendCommand("torrent-set", obj);
    }

    /**
     * Removes given torrents
     *
     * @param ids numerical ids, string hashes or the ID_RECENTLY_ADDED constant
     */
    public void removeTorrents(Object[] ids, boolean deleteLocalData) throws IOException, JSONException {
        if (ids == null)
            throw new NullPointerException("At least one id is required");
        JSONObject obj = new JSONObject();
        if (ids.length == 1)
            obj.put("ids", ids[0]);
        else {
            JSONArray ary = new JSONArray();
            for (int i = 0; i < ids.length; i++)
                ary.put(ids[i]);
            obj.put("ids", ary);
        }
        obj.put("delete-local-data", deleteLocalData);
        sendCommand("torrent-remove", obj);
    }

    /**
     * @param ids numerical ids, string hashes or the ID_RECENTLY_ADDED constant
     */
    public void moveTorrents(Object[] ids, String location, boolean move) throws IOException, JSONException {
        if (ids == null)
            throw new NullPointerException("At least one id is required");
        JSONObject obj = new JSONObject();
        if (ids.length == 1)
            obj.put("ids", ids[0]);
        else {
            JSONArray ary = new JSONArray();
            for (int i = 0; i < ids.length; i++)
                ary.put(ids[i]);
            obj.put("ids", ary);
        }
        obj.put("location", location);
        obj.put("move", move);
        sendCommand("torrent-set-location", obj);
    }

    private static SessionField[] SET_SESSION_DISALLOWED = new TransmissionSession.SessionField[]{
            SessionField.blocklistSize, SessionField.configDir, SessionField.rpcVersion, SessionField.rpcVersionMinimum,
            SessionField.version};

    /**
     * @param pairs one or more pair of TransmissionSession.SessionField and value. All
     *              SessionFields are valid, except: blocklistSize, configDir, rpcVersion,
     *              rpcVersionMinimum, and version. An error will be surfaced if those are included
     */
    public void setSession(SessionPair... pairs) throws IOException, JSONException {
        if (pairs == null)
            throw new NullPointerException("At least one pair is required");
        JSONObject obj = new JSONObject();
        for (int i = 0; i < pairs.length; i++) {
            SessionField curr = pairs[i].field;
            for (int j = 0; j < SET_SESSION_DISALLOWED.length; j++) {
                if (SET_SESSION_DISALLOWED[j] == curr)
                    throw new IllegalArgumentException("Disallowed: " + curr.name());
            }
            obj.put(TransmissionSession.FIELD_NAMES[curr.ordinal()], pairs[i].value);
        }
        sendCommand("session-set", obj);
    }

    public Map<SessionField, Object> getSession() throws IOException, JSONException {
        JSONObject result = sendCommand("session-get", null);
        Map<SessionField, Object> valByField = new HashMap<SessionField, Object>();
        for (int i = 0; i < TransmissionSession.FIELD_NAMES.length; i++) {
            String curr = TransmissionSession.FIELD_NAMES[i];
            Object val = result.get(curr);
            valByField.put(SessionField.values()[i], val);
        }
        return valByField;
    }

    /**
     * @return session status
     */
    public SessionStatus getSessionStats() throws IOException, JSONException {
        return new SessionStatus(sendCommand("session-stats", null));
    }

    public int updateBlocklist() throws IOException, JSONException {
        return sendCommand("session-stats", null).getInt("blocklist-size");
    }

    /**
     * This method tests to see if your incoming peer port is accessible from the
     * outside world.
     *
     * @return true if incoming peer port is accessible from outside
     */
    public boolean isPortOpen() throws IOException, JSONException {
        return sendCommand("port-test", null).getBoolean("port-is-open");
    }

}