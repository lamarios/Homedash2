package com.ftpix.homedash.models.export;

import com.ftpix.homedash.models.ModuleLayout;

public class ModuleLayoutExport {
    public String size;
    public int layoutId, x, y;

    public static ModuleLayoutExport fromModel(ModuleLayout moduleLayout) {
        ModuleLayoutExport export = new ModuleLayoutExport();

        export.size = moduleLayout.getSize();
        export.x = moduleLayout.getX();
        export.y = moduleLayout.getY();

        export.layoutId = moduleLayout.getLayout().getId();

        return export;
    }
}
