package ca.benow.transmission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TorrentParameters {

  public static enum Priority {
    Low, Normal, High
  };

  private static Map<Priority, Integer> prioNumByPriority = new HashMap<Priority, Integer>();
  static {
    prioNumByPriority.put(Priority.Low, -1);
    prioNumByPriority.put(Priority.Normal, 0);
    prioNumByPriority.put(Priority.High, 1);
  }

  String location;
  Integer peerLimit;
  Priority bandwidthPriority;
  /** index of wanted files.  If empty, all wanted */
  public List<Integer> filesWanted = new ArrayList<Integer>();
  /** index of unwanted files. */
  public List<Integer> filesUnwanted = new ArrayList<Integer>();
  /** index of files to be downloaded at low priority */
  public List<Integer> priorityLow = new ArrayList<Integer>();
  /** index of files to be downloaded at normal priority */
  public List<Integer> priorityNormal = new ArrayList<Integer>();
  /** index of files to be downloaded at high priority */
  public List<Integer> priorityHigh = new ArrayList<Integer>();

  protected TorrentParameters() {
    super();
  }

  /**
   * @param downloadDir path to download the torrent to
   * @return this
   */
  public TorrentParameters setLocation(String location) {
    this.location = location;
    return this;
  }

  /**
   * @param peerLimit maximum number of peers
   * @return this
   */
  public TorrentParameters setPeerLimit(int peerLimit) {
    this.peerLimit = peerLimit;
    return this;
  }

  /**
   * @param bandwidthPriority the priority of the torrent
   * @return 
   */
  public TorrentParameters setBandwidthPriority(Priority bandwidthPriority) {
    this.bandwidthPriority = bandwidthPriority;
    return this;
  }

}
