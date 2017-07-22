package com.ftpix.homedash.models;

import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Created by gz on 7/22/17.
 */
public class Version implements Comparable<Version>, Comparator<Version> {
    private int major, minor, patch;


    private final static String VERSION_PATTERN = "(\\d+)\\.(\\d+)\\.(\\d+)";


    public Version(String version) {
        if (version.matches(VERSION_PATTERN)) {

            int[] ints = Stream.of(version.split("\\.")).mapToInt(Integer::valueOf).toArray();

            major = ints[0];
            minor = ints[1];
            patch = ints[2];

        }
    }

    public Version(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public int getPatch() {
        return patch;
    }

    public void setPatch(int patch) {
        this.patch = patch;
    }

    @Override
    public int compareTo(Version version) {

        int majorCompare = Integer.compare(major, version.getMajor());
        int minorCompare = Integer.compare(minor, version.getMinor());
        int patchCompare = Integer.compare(patch, version.getPatch());
        if (majorCompare == 0) {
            if (minorCompare == 0) {
                return patchCompare;
            } else {
                return minorCompare;
            }
        } else {
            return majorCompare;
        }
    }

    @Override
    public int compare(Version version, Version t1) {
        return version.compareTo(t1);
    }

    @Override
    public String toString() {
        return major+"."+minor+"."+patch;
    }
}
