package com.ftpix.homedash.plugins.dynamicdns.providers;

import com.ftpix.homedash.plugins.dynamicdns.inputs.FormInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.gsonfire.annotations.ExposeMethodResult;


public interface DynDNSProvider {
    boolean updateIP(String ip);

   @ExposeMethodResult("form")
    ArrayList<FormInput> getForm();


    void setData(Map<String, String> data);

    Map<String, String> getData();

    @ExposeMethodResult("name")
    String getName();

    @ExposeMethodResult("id")
    String getId();

    @ExposeMethodResult("hostname")
    String getHostname();


}
