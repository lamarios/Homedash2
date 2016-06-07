package com.ftpix.homedash.Utils;

/**
 * Created by gz on 07-Jun-16.
 */
public class ByteUtils {

    public static  String humanReadableByteCount(long maxBytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (maxBytes < unit)
            return maxBytes + " B";
        int exp = (int) (Math.log(maxBytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.2f %sB", maxBytes / Math.pow(unit, exp), pre);
    }

    public static  String humanReadableByteCount(long usedBytes, long maxBytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (maxBytes < unit)
            return maxBytes + " B";
        int exp = (int) (Math.log(maxBytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.2f / %.2f %sB", usedBytes / Math.pow(unit, exp), maxBytes / Math.pow(unit, exp), pre);
    }
}
