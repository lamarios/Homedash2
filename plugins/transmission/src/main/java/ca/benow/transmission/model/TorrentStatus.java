package ca.benow.transmission.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class TorrentStatus extends JSONAccessor {

    public enum StatusField {
        stopped, finished, seeding, seedWait, downloading, downloadWait, checking, checkWait
    }

    public static StatusField parseStatus(int value, int rpcVersion) throws JSONException {
        if (rpcVersion < 14) {
            switch (value) {
                case STATUS_13_FINISHED:
                    return StatusField.finished;
                case STATUS_13_SEEDING:
                    return StatusField.seeding;
                case STATUS_13_DOWNLOADING:
                    return StatusField.downloading;
                case STATUS_13_CHECKING:
                    return StatusField.checking;
                case STATUS_13_CHECK_WAIT:
                    return StatusField.checkWait;
                default:
                    throw new JSONException("Unknown status : " + value);
            }
        } else {
            switch (value) {
                case STATUS_14_STOPPED:
                    return StatusField.stopped;
                case STATUS_14_SEEDING:
                    return StatusField.seeding;
                case STATUS_14_SEED_WAIT:
                    return StatusField.seedWait;
                case STATUS_14_DOWNLOADING:
                    return StatusField.downloading;
                case STATUS_14_DOWNLOAD_WAIT:
                    return StatusField.downloadWait;
                case STATUS_14_CHECKING:
                    return StatusField.checking;
                case STATUS_14_CHECK_WAIT:
                    return StatusField.checkWait;
                default:
                    throw new JSONException("Unknown status : " + value);
            }
        }
    }


    public static String getStatusString(int value, int rpcVersion) throws JSONException {
        if (rpcVersion < 14) {
            switch (value) {
                case STATUS_13_FINISHED:
                    return "Finished";
                case STATUS_13_SEEDING:
                    return "Seeding";
                case STATUS_13_DOWNLOADING:
                    return "Downloading";
                case STATUS_13_CHECKING:
                    return "Checking";
                case STATUS_13_CHECK_WAIT:
                    return "Check waiting";
                default:
                    throw new JSONException("Unknown status : " + value);
            }
        } else {
            switch (value) {
                case STATUS_14_STOPPED:
                    return "Stopped";
                case STATUS_14_SEEDING:
                    return "Seeding";
                case STATUS_14_SEED_WAIT:
                    return "Seed waiting";
                case STATUS_14_DOWNLOADING:
                    return "Downloading";
                case STATUS_14_DOWNLOAD_WAIT:
                    return "Download waiting";
                case STATUS_14_CHECKING:
                    return "Checking";
                case STATUS_14_CHECK_WAIT:
                    return "Check waiting";
                default:
                    throw new JSONException("Unknown status : " + value);
            }
        }
    }

    /**
     * https://github.com/killemov/Shift/blob/master/shift.js#L864
     * +   if version < 2.4
     * 1: "Check waiting",
     * 2: "Checking",
     * 4: "Downloading",
     * 8: "Seeding",
     * 16: "Stopped"
     * +   else
     * 0: "Stopped",
     * 1: "Check waiting",
     * 2: "Checking",
     * 3: "Download waiting",
     * 4: "Downloading",
     * 5: "Seed waiting",
     * 6: "Seeding"
     */

    // rpc version 13 status
    private static final int STATUS_13_FINISHED = 16;
    private static final int STATUS_13_SEEDING = 8;
    private static final int STATUS_13_DOWNLOADING = 4;
    private static final int STATUS_13_CHECKING = 2;
    private static final int STATUS_13_CHECK_WAIT = 1;

    // rpc version 14 status
    private static final int STATUS_14_SEEDING = 6;
    private static final int STATUS_14_SEED_WAIT = 5;
    private static final int STATUS_14_DOWNLOADING = 4;
    private static final int STATUS_14_DOWNLOAD_WAIT = 3;
    private static final int STATUS_14_CHECKING = 2;
    private static final int STATUS_14_CHECK_WAIT = 1;
    private static final int STATUS_14_STOPPED = 0;

    /**
     * <pre>
     * activityDate                | number                      | tr_stat
     * addedDate                   | number                      | tr_stat
     * bandwidthPriority           | number                      | tr_priority_t
     * comment                     | string                      | tr_info
     * corruptEver                 | number                      | tr_stat
     * creator                     | string                      | tr_info
     * dateCreated                 | number                      | tr_info
     * desiredAvailable            | number                      | tr_stat
     * doneDate                    | number                      | tr_stat
     * downloadDir                 | string                      | tr_torrent
     * downloadedEver              | number                      | tr_stat
     * downloadLimit               | number                      | tr_torrent
     * downloadLimited             | boolean                     | tr_torrent
     * error                       | number                      | tr_stat
     * errorString                 | string                      | tr_stat
     * eta                         | number                      | tr_stat
     * files                       | array (see below)           | n/a
     * fileStats                   | array (see below)           | n/a
     * hashString                  | string                      | tr_info
     * haveUnchecked               | number                      | tr_stat
     * haveValid                   | number                      | tr_stat
     * honorsSessionLimits         | boolean                     | tr_torrent
     * id                          | number                      | tr_torrent
     * isFinished                  | boolean                     | tr_stat
     * isPrivate                   | boolean                     | tr_torrent
     * isStalled                   | boolean                     | tr_stat
     * leftUntilDone               | number                      | tr_stat
     * magnetLink                  | number                      | n/a
     * manualAnnounceTime          | number                      | tr_stat
     * maxConnectedPeers           | number                      | tr_torrent
     * metadataPercentComplete     | double                      | tr_stat
     * name                        | string                      | tr_info
     * peer-limit                  | number                      | tr_torrent
     * peers                       | array (see below)           | n/a
     * peersConnected              | number                      | tr_stat
     * peersFrom                   | object (see below)          | n/a
     * peersGettingFromUs          | number                      | tr_stat
     * peersSendingToUs            | number                      | tr_stat
     * percentDone                 | double                      | tr_stat
     * pieces                      | string (see below)          | tr_torrent
     * pieceCount                  | number                      | tr_info
     * pieceSize                   | number                      | tr_info
     * priorities                  | array (see below)           | n/a
     * queuePosition               | number                      | tr_stat
     * rateDownload (B/s)          | number                      | tr_stat
     * rateUpload (B/s)            | number                      | tr_stat
     * recheckProgress             | double                      | tr_stat
     * secondsDownloading          | number                      | tr_stat
     * secondsSeeding              | number                      | tr_stat
     * seedIdleLimit               | number                      | tr_torrent
     * seedIdleMode                | number                      | tr_inactvelimit
     * seedRatioLimit              | double                      | tr_torrent
     * seedRatioMode               | number                      | tr_ratiolimit
     * sizeWhenDone                | number                      | tr_stat
     * startDate                   | number                      | tr_stat
     * status                      | number                      | tr_stat
     * trackers                    | array (see below)           | n/a
     * trackerStats                | array (see below)           | n/a
     * totalSize                   | number                      | tr_info
     * torrentFile                 | string                      | tr_info
     * uploadedEver                | number                      | tr_stat
     * uploadLimit                 | number                      | tr_torrent
     * uploadLimited               | boolean                     | tr_torrent
     * uploadRatio                 | double                      | tr_stat
     * wanted                      | array (see below)           | n/a
     * webseeds                    | array (see below)           | n/a
     * webseedsSendingToUs         | number                      | tr_stat
     * |                             |
     * |                             |
     * -------------------+--------+-----------------------------+
     * files              | array of objects, each containing:   |
     * +-------------------------+------------+
     * | bytesCompleted          | number     | tr_torrent
     * | length                  | number     | tr_info
     * | name                    | string     | tr_info
     * -------------------+--------------------------------------+
     * fileStats          | a file's non-constant properties.    |
     * | array of tr_info.filecount objects,  |
     * | each containing:                     |
     * +-------------------------+------------+
     * | bytesCompleted          | number     | tr_torrent
     * | wanted                  | boolean    | tr_info
     * | priority                | number     | tr_info
     * -------------------+--------------------------------------+
     * peers              | array of objects, each containing:   |
     * +-------------------------+------------+
     * | address                 | string     | tr_peer_stat
     * | clientName              | string     | tr_peer_stat
     * | clientIsChoked          | boolean    | tr_peer_stat
     * | clientIsInterested      | boolean    | tr_peer_stat
     * | flagStr                 | string     | tr_peer_stat
     * | isDownloadingFrom       | boolean    | tr_peer_stat
     * | isEncrypted             | boolean    | tr_peer_stat
     * | isIncoming              | boolean    | tr_peer_stat
     * | isUploadingTo           | boolean    | tr_peer_stat
     * | isUTP                   | boolean    | tr_peer_stat
     * | peerIsChoked            | boolean    | tr_peer_stat
     * | peerIsInterested        | boolean    | tr_peer_stat
     * | port                    | number     | tr_peer_stat
     * | progress                | double     | tr_peer_stat
     * | rateToClient (B/s)      | number     | tr_peer_stat
     * | rateToPeer (B/s)        | number     | tr_peer_stat
     * -------------------+--------------------------------------+
     * peersFrom          | an object containing:                |
     * +-------------------------+------------+
     * | fromCache               | number     | tr_stat
     * | fromDht                 | number     | tr_stat
     * | fromIncoming            | number     | tr_stat
     * | fromLpd                 | number     | tr_stat
     * | fromLtep                | number     | tr_stat
     * | fromPex                 | number     | tr_stat
     * | fromTracker             | number     | tr_stat
     * -------------------+--------------------------------------+
     * pieces             | A bitfield holding pieceCount flags  | tr_torrent
     * | which are set to 'true' if we have   |
     * | the piece matching that position.    |
     * | JSON doesn't allow raw binary data,  |
     * | so this is a base64-encoded string.  |
     * -------------------+--------------------------------------+
     * priorities         | an array of tr_info.filecount        | tr_info
     * | numbers. each is the tr_priority_t   |
     * | mode for the corresponding file.     |
     * -------------------+--------------------------------------+
     * trackers           | array of objects, each containing:   |
     * +-------------------------+------------+
     * | announce                | string     | tr_tracker_info
     * | id                      | number     | tr_tracker_info
     * | scrape                  | string     | tr_tracker_info
     * | tier                    | number     | tr_tracker_info
     * -------------------+--------------------------------------+
     * trackerStats       | array of objects, each containing:   |
     * +-------------------------+------------+
     * | announce                | string     | tr_tracker_stat
     * | announceState           | number     | tr_tracker_stat
     * | downloadCount           | number     | tr_tracker_stat
     * | hasAnnounced            | boolean    | tr_tracker_stat
     * | hasScraped              | boolean    | tr_tracker_stat
     * | host                    | string     | tr_tracker_stat
     * | id                      | number     | tr_tracker_stat
     * | isBackup                | boolean    | tr_tracker_stat
     * | lastAnnouncePeerCount   | number     | tr_tracker_stat
     * | lastAnnounceResult      | string     | tr_tracker_stat
     * | lastAnnounceStartTime   | number     | tr_tracker_stat
     * | lastAnnounceSucceeded   | boolean    | tr_tracker_stat
     * | lastAnnounceTime        | number     | tr_tracker_stat
     * | lastAnnounceTimedOut    | boolean    | tr_tracker_stat
     * | lastScrapeResult        | string     | tr_tracker_stat
     * | lastScrapeStartTime     | number     | tr_tracker_stat
     * | lastScrapeSucceeded     | boolean    | tr_tracker_stat
     * | lastScrapeTime          | number     | tr_tracker_stat
     * | lastScrapeTimedOut      | boolean    | tr_tracker_stat
     * | leecherCount            | number     | tr_tracker_stat
     * | nextAnnounceTime        | number     | tr_tracker_stat
     * | nextScrapeTime          | number     | tr_tracker_stat
     * | scrape                  | string     | tr_tracker_stat
     * | scrapeState             | number     | tr_tracker_stat
     * | seederCount             | number     | tr_tracker_stat
     * | tier                    | number     | tr_tracker_stat
     * -------------------+-------------------------+------------+
     * wanted             | an array of tr_info.fileCount        | tr_info
     * | 'booleans' true if the corresponding |
     * | file is to be downloaded.            |
     * -------------------+--------------------------------------+
     * webseeds           | an array of strings:                 |
     * +-------------------------+------------+
     * | webseed                 | string     | tr_info
     * +-------------------------+------------+
     * </pre>
     *
     * @author andy
     */
    public enum TorrentField {
        all, activityDate, addedDate, bandwidthPriority, comment, corruptEver, creator, dateCreated, desiredAvailable, doneDate, downloadDir, downloadedEver, downloadLimit, downloadLimited, error, errorString, eta, files, fileStats, hashString, haveUnchecked, haveValid, honorsSessionLimits, id, isFinished, isPrivate, leftUntilDone, magnetLink, manualAnnounceTime, maxConnectedPeers, metadataPercentComplete, name, peerLimit, peers, peersConnected, peersFrom, peersGettingFromUs, peersSendingToUs, percentDone, pieces, pieceCount, pieceSize, priorities, rateDownload, rateUpload, recheckProgress, seedIdleLimit, seedIdleMode, seedRatioLimit, seedRatioMode, sizeWhenDone, startDate, status, trackers, trackerStats, totalSize, torrentFile, uploadedEver, uploadLimit, uploadLimited, uploadRatio, wanted, webseeds, TorrentField, webseedsSendingToUs
    }

    public static TorrentField[] defaultFields = new TorrentField[]{TorrentField.id, TorrentField.name,
            TorrentField.status, TorrentField.percentDone};

    public static String[] fieldNameByFieldPos = new String[]{"all", "activityDate", "addedDate", "bandwidthPriority",
            "comment", "corruptEver", "creator", "dateCreated", "desiredAvailable", "doneDate", "downloadDir",
            "downloadedEver", "downloadLimit", "downloadLimited", "error", "errorString", "eta", "files", "fileStats",
            "hashString", "haveUnchecked", "haveValid", "honorsSessionLimits", "id", "isFinished", "isPrivate",
            "leftUntilDone", "magnetLink", "manualAnnounceTime", "maxConnectedPeers", "metadataPercentComplete", "name",
            "peerLimit", "peers", "peersConnected", "peersFrom", "peersGettingFromUs", "peersSendingToUs", "percentDone",
            "pieces", "pieceCount", "pieceSize", "priorities", "rateDownload", "rateUpload", "recheckProgress",
            "seedIdleLimit", "seedIdleMode", "seedRatioLimit", "seedRatioMode", "sizeWhenDone", "startDate", "status",
            "trackers", "trackerStats", "totalSize", "torrentFile", "uploadedEver", "uploadLimit", "uploadLimited",
            "uploadRatio", "wanted", "webseeds", "webseedsSendingToUs"};

    private int rpcVersion = 0;

    public TorrentStatus(JSONObject jsonObject, int rpcVersion) {
        super(jsonObject);
        this.rpcVersion = rpcVersion;
    }

    public JSONObject getJSONObject() {
        return obj;
    }

    public Object getField(TorrentField field) throws JSONException {
        return obj.get(getFieldName(field));
    }

    private String getFieldName(TorrentField field) {
        return fieldNameByFieldPos[field.ordinal()];
    }

    public int getId() throws JSONException {
        return (Integer) getField(TorrentField.id);
    }

    public double getPercentDone() throws JSONException {
        return (Double) getField(TorrentField.percentDone);
    }

    public StatusField getStatus() throws JSONException {
        return parseStatus((Integer) getField(TorrentField.status), rpcVersion);
    }

    public String getName() throws JSONException {
        return (String) getField(TorrentField.name);
    }

    public Date getDateField(TorrentField field) throws JSONException {
        return new Date((Long) getField(field));
    }

    @Override
    public String toString() {
        try {
            return obj.toString(2);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
}
