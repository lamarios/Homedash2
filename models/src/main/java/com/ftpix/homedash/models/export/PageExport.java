package com.ftpix.homedash.models.export;

import com.ftpix.homedash.models.Page;

public class PageExport {
    public int id;
    public String name;

    public static PageExport fromModel(Page page) {
        PageExport export = new PageExport();

        export.id = page.getId();
        export.name = page.getName();

        return export;
    }

    public static Page toModel(PageExport export) {
        Page page = new Page();
        page.setName(export.name);
        page.setId(export.id);


        return page;

    }
}
