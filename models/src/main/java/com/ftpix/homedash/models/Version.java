package com.ftpix.homedash.models;

import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Created by gz on 7/22/17.
 */
public class Version implements Comparable<Version>, Comparator<Version> {
    private int year = 0, month = 0, patch = 0;


    private final static String VERSION_PATTERN = "(\\d+)\\.(\\d+)\\.(\\d+)";


    public Version(String version) {
        if (version.matches(VERSION_PATTERN)) {

            int[] ints = Stream.of(version.split("\\.")).mapToInt(Integer::valueOf).toArray();

            year = ints[0];
            month = ints[1];
            patch = ints[2];
        }
    }

    public Version(int major, int minor, int patch) {
        this.year = major;
        this.month = minor;
        this.patch = patch;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    @Override
    public int compareTo(Version version) {

        int yearCompare = Integer.compare(year, version.getYear());
        int monthCompare = Integer.compare(month, version.getMonth());
        int patchCompare = Integer.compare(patch, version.getPatch());

        if (yearCompare == 0) {
            if (monthCompare == 0) {
                return patchCompare;
            } else {
                return monthCompare;
            }
        } else {
            return yearCompare;
        }
    }

    @Override
    public int compare(Version version, Version t1) {
        return version.compareTo(t1);
    }

    @Override
    public String toString() {
        return year + "." + month + "." + patch;
    }

    public int getPatch() {
        return patch;
    }
}
