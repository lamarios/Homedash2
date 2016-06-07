package ca.benow.transmission;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ca.benow.transmission.model.TrackerPair;

public class SetTorrentParameters extends TorrentParameters {

  /**
   * Indicates download or upload is unlimited
   */
  public static final int UNLIMITED = -1;

  List<Object> ids = new ArrayList<Object>();
  int downloadLimit = UNLIMITED;
  boolean honorsSessionLimits = false;
  int seedIdleLimit;
  int seedIdleMode;
  double seedRatioLimit;
  int seedRatioMode;
  List<String> trackerAdd = new ArrayList<String>();
  List<Integer> trackerRemove = new ArrayList<Integer>();
  List<TrackerPair> trackerReplace = new ArrayList<TrackerPair>();
  int uploadLimit = UNLIMITED;

  public SetTorrentParameters(Object id) {
    super();
    this.ids.add(id);
  }

  public SetTorrentParameters(List<Object> ids) {
    this.ids.addAll(ids);
  }

  /**
   * @param downloadLimit maximum download speed (KBps) (or {@link #UNLIMITED})
   * @return this
   */
  public SetTorrentParameters setDownloadLimit(int downloadLimit) {
    this.downloadLimit = downloadLimit;
    return this;
  }

  /**
   * @param honorsSessionLimits true if session upload limits are honored
   * @return this 
   */
  public SetTorrentParameters setHonorsSessionLimits(boolean honorsSessionLimits) {
    this.honorsSessionLimits = honorsSessionLimits;
    return this;
  }

  /**
   * @param seedIdleLimit which seeding inactivity to use. See tr_inactvelimit
   * @return this
   */
  public SetTorrentParameters setSeedIdleLimit(int seedIdleLimit) {
    this.seedIdleLimit = seedIdleLimit;
    return this;
  }

  /**
   * @param seedIdleMode which ratio to use. See tr_ratiolimit
   * @return this
   */
  public SetTorrentParameters setSeedIdleMode(int seedIdleMode) {
    this.seedIdleMode = seedIdleMode;
    return this;
  }

  /**
   * @param seedRatioLimit torrent-level seeding ratio
   * @return this
   */
  public SetTorrentParameters setSeedRatioLimit(double seedRatioLimit) {
    this.seedRatioLimit = seedRatioLimit;
    return this;
  }

  /**
   * @param seedRatioMode which ratio to use. See tr_ratiolimit
   * @return this
   */
  public SetTorrentParameters setSeedRatioMode(int seedRatioMode) {
    this.seedRatioMode = seedRatioMode;
    return this;
  }

  /**
   * @param uploadLimit maximum upload speed (KBps) (or {@link #UNLIMITED})
   * @return this
   */
  public SetTorrentParameters setUploadLimit(int uploadLimit) {
    this.uploadLimit = uploadLimit;
    return this;
  }

  public JSONObject toRequestObject() throws JSONException {
    JSONObject obj = new JSONObject();
    if (!ids.isEmpty()) {
      JSONArray ary = new JSONArray(ids);
      obj.put("ids", ary);
    }
    obj.put("bandwidthPriority", bandwidthPriority);
    if (downloadLimit == SetTorrentParameters.UNLIMITED)
      obj.put("downloadLimited", false);
    else {
      obj.put("downloadLimit", downloadLimit);
      obj.put("downloadLimited", true);
    }
    if (!filesWanted.isEmpty()) {
      JSONArray ary = new JSONArray(filesWanted);
      obj.put("files-wanted", ary);
    }
    if (!filesUnwanted.isEmpty()) {
      JSONArray ary = new JSONArray(filesUnwanted);
      obj.put("files-unwanted", ary);
    }
    obj.put("honorsSessionLimits", honorsSessionLimits);
    if (location != null)
      obj.put("location", location);
    if (peerLimit != null)
      obj.put("peer-limit", peerLimit);
    if (!priorityHigh.isEmpty()) {
      JSONArray ary = new JSONArray(priorityHigh);
      obj.put("priority-high", ary);
    }
    if (!priorityLow.isEmpty()) {
      JSONArray ary = new JSONArray(priorityLow);
      obj.put("priority-low", ary);
    }
    if (!priorityNormal.isEmpty()) {
      JSONArray ary = new JSONArray(priorityNormal);
      obj.put("priority-normal", ary);
    }

    obj.put("seedIdleLimit", seedIdleLimit);
    obj.put("seedIdleMode", seedIdleMode);
    obj.put("seedRatioLimit", seedRatioLimit);
    obj.put("seedRatioMode", seedRatioMode);
    if (!trackerAdd.isEmpty()) {
      JSONArray ary = new JSONArray(priorityNormal);
      obj.put("trackerAdd", ary);
    }
    if (!trackerRemove.isEmpty()) {
      JSONArray ary = new JSONArray(trackerRemove);
      obj.put("trackerRemove", ary);
    }
    if (!trackerReplace.isEmpty()) {
      JSONArray ary = new JSONArray();
      for (TrackerPair pair : trackerReplace) {
        JSONArray ary2 = new JSONArray();
        ary2.put(pair.id);
        ary2.put(pair.newURL);
        ary.put(ary2);
      }
      obj.put("trackerReplace", ary);
    }
    if (uploadLimit == SetTorrentParameters.UNLIMITED)
      obj.put("uploadLimited", false);
    else {
      obj.put("uploadLimit", uploadLimit);
      obj.put("uploadLimited", true);
    }
    return obj;
  }

}
