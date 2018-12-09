package com.ftpix.homedash.models.export;

import com.ftpix.homedash.models.Layout;

public class LayoutExport {
    public int id, maxGridWidth;
    public String name;


    public static LayoutExport fromModel(Layout layout) {
        LayoutExport export = new LayoutExport();

        export.id = layout.getId();
        export.maxGridWidth = layout.getMaxGridWidth();
        export.name = layout.getName();

        return export;
    }

    public static Layout toModel(LayoutExport export) {
        Layout layout = new Layout();

        layout.setMaxGridWidth(export.maxGridWidth);
        layout.setName(export.name);
        layout.setId(export.id);

        return layout;
    }

}
