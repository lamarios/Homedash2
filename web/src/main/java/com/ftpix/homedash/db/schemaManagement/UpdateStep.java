package com.ftpix.homedash.db.schemaManagement;

import com.ftpix.homedash.models.Version;

import java.util.List;

/**
 * Created by gz on 7/22/17.
 */
public interface UpdateStep {

    List<String> ups();


    Version getVersion();
}
