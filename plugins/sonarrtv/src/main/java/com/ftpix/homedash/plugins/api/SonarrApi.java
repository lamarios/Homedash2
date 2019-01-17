package com.ftpix.homedash.plugins.api;


import com.ftpix.homedash.plugins.api.models.SearchResults;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.ftpix.homedash.plugins.api.models.SonarrCalendar;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.apache.commons.jexl2.parser.ASTStringLiteral;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.util.UrlEncoded;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by gz on 06-Jun-16.
 */
public class SonarrApi {
    private static Logger logger = LogManager.getLogger();


    private String url, apiKey;
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    private Gson gson = new GsonBuilder().create();

    public SonarrApi(String url, String apiKey) {
        if (!url.endsWith("/"))
            url += "/";
        if (!url.startsWith("http://") && !url.startsWith("https://"))
            url = "http://" + url;
        url += "api";
        logger.info("Creating SonarrAPI instance url:[{}] key:[{}]", url,
                apiKey);
        this.url = url;
        this.apiKey = apiKey;
    }

    public List<SonarrCalendar> getCalendar(Date from, Date to)
            throws IOException, SonarrUnauthorizedException {
        return getCalendar(from, to, true);
    }

    public List<SonarrCalendar> getCalendar(Date from, Date to,
                                            boolean keepDuplicates) throws IOException,
            SonarrUnauthorizedException {
        logger.info("[Sonarr] Calling {}/Calendar from:{}, to:{}", url, from, to);

        String url = this.url + "/Calendar?apiKey=" + this.apiKey;
        if (from != null)
            url += "&start=" + df.format(from);
        if (to != null)
            url += "&end=" + df.format(to);

        List<SonarrCalendar> calendar = new ArrayList<>();

        //keeping track of series inserted
        Set<Integer> inserted = new HashSet<Integer>();
        try {

            String response = Unirest.get(url).asString().getBody();

            logger.info("Response from Sonarr: \n {}", response);

            JsonParser parser = new JsonParser();
            JsonArray json = (JsonArray) parser.parse(response);

            Iterator<JsonElement> iterator = json.iterator();

            iterator.forEachRemaining((item) -> {

                SonarrCalendar tmp = new SonarrCalendar();
                JsonObject calItem = item.getAsJsonObject();
                JsonObject series = calItem.get("series").getAsJsonObject();

                int seriesId = series.get("tvdbId").getAsInt();

                if (keepDuplicates || !inserted.contains(seriesId)) {

                    tmp.setAirDate(calItem.get("airDate").getAsString());
                    tmp.setEpisodeNumber(calItem.get("episodeNumber")
                            .getAsInt());
                    tmp.setNetwork(series.get("network").getAsString());
                    try {
                        tmp.setOverview(calItem.get("overview").getAsString());
                    } catch (NullPointerException e) {

                    }
                    tmp.setSeriesName(series.get("title").getAsString());
                    tmp.setSeriesId(seriesId);
                    tmp.setEpisodeName(calItem.get("title").getAsString());


                    Iterator<JsonElement> images = series.get("images")
                            .getAsJsonArray().iterator();

                    images.forEachRemaining((image) -> {
                        JsonObject imageObject = image.getAsJsonObject();
                        String type = imageObject.get("coverType")
                                .getAsString();
                        String imageUrl = imageObject.get("url").getAsString();
                        if (type.equalsIgnoreCase("fanart")) {
                            tmp.setFanart(imageUrl);
                        } else if (type.equalsIgnoreCase("poster")) {
                            tmp.setPoster(imageUrl);
                        } else if (type.equalsIgnoreCase("banner")) {
                            tmp.setBanner(imageUrl);
                        }
                    });

                    calendar.add(tmp);
                    inserted.add(seriesId);
                }
            });

            return calendar;
        } catch (UnirestException e) {
            logger.info("Error:" + e.getMessage());
            throw new SonarrUnauthorizedException();
        }
    }

    public boolean checkApi() throws IOException, SonarrUnauthorizedException {
        logger.info("[Sonarr] Calling /System/Status");

        String url = this.url + "/System/Status?apiKey=" + this.apiKey;

        try {
            String response = Unirest.get(url).asString().getBody();
            logger.info("response: [{}]", response);

            if (response.contains("error")) {
                logger.info("Not authorized");
                throw new SonarrUnauthorizedException();
            }

        } catch (UnirestException e) {
            logger.info("Error:" + e.getMessage());
            throw new IOException();
        }

        return true;
    }


    /**
     * Search for series to add
     *
     * @param query
     * @return
     * @throws SonarrUnauthorizedException
     * @throws IOException
     */
    public String searchSeries(String query) throws SonarrUnauthorizedException, IOException {
        logger.info("[Sonarr] Calling /series/lookup");

        query = URLEncoder.encode(query, StandardCharsets.UTF_8.name());

        String url = this.url + "/series/lookup?term=" + query + "&apiKey=" + this.apiKey;
        try {
            String response = Unirest.get(url).asString().getBody();
            logger.info("response: [{}]", response);

            if (response.contains("error")) {
                logger.info("Not authorized");
                throw new SonarrUnauthorizedException();
            }

            return response;
        } catch (UnirestException e) {
            logger.info("Error:" + e.getMessage());
            throw new IOException();
        }
    }

    public String getQualities() throws SonarrUnauthorizedException, IOException {
        String url = this.url + "/profile?apiKey=" + this.apiKey;
        try {
            String response = Unirest.get(url).asString().getBody();
            logger.info("response: [{}]", response);

            if (response.contains("error")) {
                logger.info("Not authorized");
                throw new SonarrUnauthorizedException();
            }

            return response;
        } catch (UnirestException e) {
            logger.info("Error:" + e.getMessage());
            throw new IOException();
        }
    }


    public String getFolders() throws SonarrUnauthorizedException, IOException {
        String url = this.url + "/rootfolder?apiKey=" + this.apiKey;
        try {
            String response = Unirest.get(url).asString().getBody();
            logger.info("response: [{}]", response);

            if (response.contains("error")) {
                logger.info("Not authorized");
                throw new SonarrUnauthorizedException();
            }

            return response;
        } catch (UnirestException e) {
            logger.info("Error:" + e.getMessage());
            throw new IOException();
        }
    }


    /**
     * adds a tv show to sonnarr
     * @param showJson the show JSON no manipulation is required so we keep it as is
     * @return true if everything goes right
     * @throws IOException
     * @throws SonarrUnauthorizedException
     */
    public boolean addShow(String showJson) throws IOException, SonarrUnauthorizedException {
        String url = this.url + "/series?apiKey=" + this.apiKey;

        try {
            String response = Unirest.post(url).body(showJson).asString().getBody();


            if (response.contains("error")) {
                logger.info("Not authorized");
                throw new SonarrUnauthorizedException();
            }

            return true;
        } catch (UnirestException e) {
            logger.info("Error:" + e.getMessage());
            throw new IOException();
        }
    }
}



