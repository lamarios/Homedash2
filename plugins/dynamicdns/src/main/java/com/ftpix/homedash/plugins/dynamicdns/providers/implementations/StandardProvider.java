package com.ftpix.homedash.plugins.dynamicdns.providers.implementations;

import com.ftpix.homedash.plugins.dynamicdns.inputs.FormInput;
import com.ftpix.homedash.plugins.dynamicdns.providers.DynDNSProvider;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class StandardProvider implements DynDNSProvider {
    protected transient String username, password, hostname;
    protected Logger logger = LogManager.getLogger();

    private static final String USERNAME = "username", PASSWORD = "password", HOSTNAME = "hostname";

    protected abstract String getUrl();

    @Override
    public void setData(Map<String, String> data) {
        username = data.get(USERNAME);
        password = data.get(PASSWORD);
        hostname = data.get(HOSTNAME);
    }

    @Override
    public List<FormInput> getForm() {
        List<FormInput> inputs = new ArrayList<>();
        inputs.add(new FormInput(USERNAME, "", "Username", FormInput.TYPE_TEXT));
        inputs.add(new FormInput(PASSWORD, "", "Password", FormInput.TYPE_PASSWORD));
        inputs.add(new FormInput(HOSTNAME, "", "Hostname", FormInput.TYPE_TEXT));

        return inputs;
    }

    @Override
    public Map<String, String> getData() {
        Map<String, String> data = new HashMap<String, String>();
        data.put(USERNAME, username);
        data.put(PASSWORD, password);
        data.put(HOSTNAME, hostname);

        return data;
    }

    @Override
    public boolean updateIP(String ip) {

        try {

            String url = this.getUrl() + (this.getUrl().contains("?") ? "&" : "?") + "hostname=" + this.hostname;

            HttpGet get = new HttpGet(url);

            //String encoding = Base64.encodeBase64String(("username" + ":" + "password").getBytes());

            //get.setHeader("Authorization", "Basic " + encoding);

            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));

            CloseableHttpClient client = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();


            get.setHeader("User-Agent", "HomeDash https://github.com/lamarios/HomeDash");

            logger.info("Sending to [{}]", url);

            HttpResponse response;
            response = client.execute(get);

            String responseStr = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            logger.info("Status[{}], Response: [{}]", response.getStatusLine().getStatusCode(), responseStr.trim());

            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK && (responseStr.contains("nochg") || responseStr.contains("good"));
        } catch (Exception e) {
            logger.error("Error while updating IP to [{}]", ip);
            return false;
        }
    }

    @Override
    public String getId() {
        return getName() + hostname + username;
    }

    @Override
    public String getHostname() {
        return hostname;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            StandardProvider o = (StandardProvider) obj;
            return this.getId().equalsIgnoreCase(o.getId());
        } catch (Exception e) {
            return false;
        }
    }
}
