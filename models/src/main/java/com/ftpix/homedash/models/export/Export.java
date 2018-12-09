package com.ftpix.homedash.models.export;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Export {

    public List<PageExport> boards = new ArrayList<>();
    public List<LayoutExport> layouts = new ArrayList<>();
    public List<ModuleExport> modules = new ArrayList<>();
    public Map<String, String> settings = new HashMap<>();

}
