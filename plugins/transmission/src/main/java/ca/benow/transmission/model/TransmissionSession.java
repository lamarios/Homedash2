package ca.benow.transmission.model;


public class TransmissionSession {

  /**
   * <pre>
   *    "alt-speed-down"                 | number     | max global download speed (KBps)
   "alt-speed-enabled"              | boolean    | true means use the alt speeds
   "alt-speed-time-begin"           | number     | when to turn on alt speeds (units: minutes after midnight)
   "alt-speed-time-enabled"         | boolean    | true means the scheduled on/off times are used
   "alt-speed-time-end"             | number     | when to turn off alt speeds (units: same)
   "alt-speed-time-day"             | number     | what day(s) to turn on alt speeds (look at tr_sched_day)
   "alt-speed-up"                   | number     | max global upload speed (KBps)
   "blocklist-url"                  | string     | location of the blocklist to use for "blocklist-update"
   "blocklist-enabled"              | boolean    | true means enabled
   "blocklist-size"                 | number     | number of rules in the blocklist
   "cache-size-mb"                  | number     | maximum size of the disk cache (MB)
   "config-dir"                     | string     | location of transmission's configuration directory
   "download-dir"                   | string     | default path to download torrents
   "dht-enabled"                    | boolean    | true means allow dht in public torrents
   "encryption"                     | string     | "required", "preferred", "tolerated"
   "idle-seeding-limit"             | number     | the default seed inactivity limit for torrents to use
   "idle-seeding-limit-enabled"     | boolean    | true if the seeding inactivity limit is honored by default
   "incomplete-dir"                 | string     | path for incomplete torrents, when enabled
   "incomplete-dir-enabled"         | boolean    | true means keep torrents in incomplete-dir until done
   "lpd-enabled"                    | boolean    | true means allow Local Peer Discovery in public torrents
   "peer-limit-global"              | number     | maximum global number of peers
   "peer-limit-per-torrent"         | number     | maximum global number of peers
   "pex-enabled"                    | boolean    | true means allow pex in public torrents
   "peer-port"                      | number     | port number
   "peer-port-random-on-start"      | boolean    | true means pick a random peer port on launch
   "port-forwarding-enabled"        | boolean    | true means enabled
   "rename-partial-files"           | boolean    | true means append ".part" to incomplete files
   "rpc-version"                    | number     | the current RPC API version
   "rpc-version-minimum"            | number     | the minimum RPC API version supported
   "script-torrent-done-filename"   | string     | filename of the script to run
   "script-torrent-done-enabled"    | boolean    | whether or not to call the "done" script
   "seedRatioLimit"                 | double     | the default seed ratio for torrents to use
   "seedRatioLimited"               | boolean    | true if seedRatioLimit is honored by default
   "speed-limit-down"               | number     | max global download speed (KBps)
   "speed-limit-down-enabled"       | boolean    | true means enabled
   "speed-limit-up"                 | number     | max global upload speed (KBps)
   "speed-limit-up-enabled"         | boolean    | true means enabled
   "start-added-torrents"           | boolean    | true means added torrents will be started right away
   "trash-original-torrent-files"   | boolean    | true means the .torrent file of added torrents will be deleted
   "units"                          | object     | see below
   "version"                        | string     | long version string "$version ($revision)"
   ---------------------------------+------------+-----------------------------+
   units                            | object containing:                       |
                                    +--------------+--------+------------------+
                                    | speed-units  | array  | 4 strings: KB/s, MB/s, GB/s, TB/s
                                    | speed-bytes  | number | number of bytes in a KB (1000 for kB; 1024 for KiB)
                                    | size-units   | array  | 4 strings: KB/s, MB/s, GB/s, TB/s
                                    | size-bytes   | number | number of bytes in a KB (1000 for kB; 1024 for KiB)
                                    | memory-units | array  | 4 strings: KB/s, MB/s, GB/s, TB/s
                                    | memory-bytes | number | number of bytes in a KB (1000 for kB; 1024 for KiB)
                                    +--------------+--------+------------------+
   * </pre>
   * @author andy
   *
   */
  public enum SessionField {
    altSpeedDown, altSpeedEnabled, altSpeedTimeBegin, altSpeedTimeEnabled, altSpeedTimeEnd, altSpeedTimeDay, altSpeedUp, blocklistURL, blocklistEnabled, blocklistSize, cacheSizeMB, configDir, downloadDir, dhtEnabled, encryption, idleSeedingLimit, idleSeedingLimitEnabled, incompleteDir, incompleteDirEnabled, lpdEnabled, peerLimitGlobal, peerLimitPerTorrent, pexEnabled, peerPort, peerPortRandomOnStart, portForwardingEnabled, renamePartialFiles, rpcVersion, rpcVersionMinimum, scriptTorrentDoneFilename, scriptTorrentDoneEnabled, seedRatioLimit, seedRatioLimited, speedLimitDown, speedLimitDownEnabled, speedLimitUp, speedLimitUpEnabled, startAddedTorrents, trashOriginalTorrentFiles, units, version
  }

  public static String[] FIELD_NAMES = new String[] { "alt-speed-down", "alt-speed-enabled", "alt-speed-time-begin",
      "alt-speed-time-enabled", "alt-speed-time-end", "alt-speed-time-day", "alt-speed-up", "blocklist-url", "blocklist-enabled",
      "blocklist-size", "cache-size-mb", "config-dir", "download-dir", "dht-enabled", "encryption", "idle-seeding-limit",
      "idle-seeding-limit-enabled", "incomplete-dir", "incomplete-dir-enabled", "lpd-enabled", "peer-limit-global",
      "peer-limit-per-torrent", "pex-enabled", "peer-port", "peer-port-random-on-start", "port-forwarding-enabled",
      "rename-partial-files", "rpc-version", "rpc-version-minimum", "script-torrent-done-filename",
      "script-torrent-done-enabled", "seedRatioLimit", "seedRatioLimited", "speed-limit-down", "speed-limit-down-enabled",
      "speed-limit-up", "speed-limit-up-enabled", "start-added-torrents", "trash-original-torrent-files", "units", "version" };

  public static class SessionPair {
    public final SessionField field;
    public final Object value;

    public SessionPair(SessionField field, Object value) {
      this.field = field;
      this.value = value;
    }
  }

}
