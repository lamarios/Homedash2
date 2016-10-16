package com.ftpix.homedash.plugins.portmapper;

/**
 * Created by gz on 12-Jun-16.
 */
public class MappingObject {
    public String protocol, name, internalIp;
    public int externalPort, internalPort;
    public boolean forced = false;

    @Override
    public boolean equals(Object obj) {
        try {
            MappingObject o = (MappingObject) obj;
            return protocol.equalsIgnoreCase(o.protocol) && externalPort == o.externalPort;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return protocol + "-" + externalPort;
    }
}
