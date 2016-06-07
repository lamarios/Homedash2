package ca.benow.transmission.model;

import org.json.JSONException;
import org.json.JSONObject;


public class SessionStatus extends JSONAccessor {

  public class SessionStats extends JSONAccessor {

    public SessionStats(JSONObject jsonObject) {
      super(jsonObject);
    }

    public int getUploadedBytes() throws JSONException {
      return obj.getInt("uploadedBytes");
    }

    public int getDownloadedBytes()  throws JSONException {
      return obj.getInt("downloadedBytes");
    }

    public int getFilesAdded() throws JSONException  {
      return obj.getInt("filesAdded");
    }

    public int getSessionCount()  throws JSONException {
      return obj.getInt("sessionCount");
    }

    public int getSecondsActive()  throws JSONException {
      return obj.getInt("secondsActive");
    }

  }

  public SessionStatus(JSONObject jsonObject) {
    super(jsonObject);
  }

  public int getActiveTorrentCount()  throws JSONException {
    return obj.getInt("activeTorrentCount");
  }

  public int getDownloadSpeed() throws JSONException  {
    return obj.getInt("downloadSpeed");
  }

  public int getPausedTorrentCount()  throws JSONException {
    return obj.getInt("pausedTorrentCount");
  }

  public int getTorrentCount()  throws JSONException {
    return obj.getInt("torrentCount");
  }

  public int getUploadSpeed()  throws JSONException {
    return obj.getInt("uploadSpeed");
  }

  public SessionStats getCumulativeStats()  throws JSONException {
    return new SessionStats(obj.getJSONObject("cumulative-stats"));
  }

  public SessionStats getCurrentStats()  throws JSONException {
    return new SessionStats(obj.getJSONObject("current-stats"));
  }
}
