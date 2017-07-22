package com.ftpix.homedash.db.schemaManagement.updates;

import com.ftpix.homedash.db.schemaManagement.UpdateStep;
import com.ftpix.homedash.models.Version;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gz on 7/22/17.
 */
public class Update20170722 implements UpdateStep {
    @Override
    public List<String> ups() {
        List<String> statements = new ArrayList<>();

        statements.add("ALTER TABLE modules ADD COLUMN IF NOT EXISTS onKiosk TINYINT(1) DEFAULT 0");

        return statements;
    }

    @Override
    public Version getVersion() {
        return new Version("2017.07.22");
    }
}
