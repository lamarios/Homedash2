package com.ftpix.homedash.plugins.unifi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.impl.client.BasicCookieStore;
import org.json.JSONObject;

import java.util.Map;

public class UnifiApi {
    private String url, site;
    private final Gson gson = new GsonBuilder().create();


    public UnifiApi(String url, String site) {
        BasicCookieStore cookieStore = new BasicCookieStore();
        Unirest.setHttpClient(org.apache.http.impl.client.HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build());

        this.url = url;
        this.site = site;
    }


    public boolean login(String username, String password) throws UnirestException {

        JSONObject creds = new JSONObject();
        creds.put("username", username);
        creds.put("password", password);


        String body = Unirest.post(url + "api/login")
                .body(creds)
                .asString()
                .getBody();


        UnifiResponse login = gson.fromJson(body, UnifiResponse.class);

        if (login.getMeta().containsKey("rc") && login.getMeta().get("rc").equalsIgnoreCase("ok")) {
            return true;
        } else {
            return false;
        }
    }


    public UnifiThroughPut getThroughput() throws UnirestException {
        String body = Unirest.get(url + "api/s/" + site + "/stat/health")
                .asString()
                .getBody();

        UnifiResponse response = gson.fromJson(body, UnifiResponse.class);


        if (response.getData().size() >= 3) {
            Map<String, Object> data = response.getData().get(2);
            UnifiThroughPut throughput = UnifiThroughPut.of(site, (double) data.get("tx_bytes-r"), (double) data.get("rx_bytes-r"), ((Double) data.get("latency")).intValue());
            return throughput;
        } else {
            return null;
        }
    }

}
