package com.ftpix.homedash.plugins.unifi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnifiResponse {
    private Map<String, String> meta = new HashMap<>();
    private List<Map<String, Object>> data;

    public Map<String, String> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, String> meta) {
        this.meta = meta;
    }

    public List<Map<String, Object>> getData() {
        return data;
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }
}
