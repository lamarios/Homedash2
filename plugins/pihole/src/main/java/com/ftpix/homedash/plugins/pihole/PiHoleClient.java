package com.ftpix.homedash.plugins.pihole;

import com.ftpix.homedash.plugins.pihole.models.AnswerType;
import com.ftpix.homedash.plugins.pihole.models.PiHoleQuery;
import com.ftpix.homedash.plugins.pihole.models.PiHoleStats;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PiHoleClient {
    private String url, auth;
    private Logger logger = LogManager.getLogger();
    private Gson gson = new Gson();

    public PiHoleClient(String url, String auth) {
        this.url = url;

        this.auth = Optional.ofNullable(auth).map(String::trim).orElse(null);
    }


    /**
     * Get public stats for the pihole installation
     *
     * @return
     * @throws UnirestException
     */
    public PiHoleStats getStats() throws UnirestException {

        HttpResponse<String> response = Unirest
                .post(this.url + "api.php")
                .header("cache-control", "no-cache").asString();

        logger.info("[PiHole] Query result: {} for url:[{}]",
                response.getBody(), this.url);
        return gson.fromJson(response.getBody(), PiHoleStats.class);

    }


    /**
     * Gets all queries
     *
     * @return
     * @throws UnauthorizedException
     * @throws UnirestException
     */
    public List<PiHoleQuery> getQueries() throws UnauthorizedException, UnirestException {
        if (auth == null) {
            throw new UnauthorizedException("No auth key set up");
        }

        HttpResponse<String> response = Unirest
                .post(this.url + "api.php?auth=" + auth + "&getAllQueries=100")
                .header("cache-control", "no-cache").asString();

        PiHoleResults data = gson.fromJson(response.getBody(), PiHoleResults.class);


        return Stream.of(Optional.ofNullable(data).map(PiHoleResults::getData).orElse(new String[0][0]))
                .map(d -> {
                    PiHoleQuery query = new PiHoleQuery();

                    Long timestamp = Long.valueOf(d[0]);
                    LocalDateTime triggerTime =
                            LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp),
                                    TimeZone.getDefault().toZoneId());

                    query.setDate(triggerTime);

                    query.setType(d[1]);
                    query.setRequestedDomain(d[2]);
                    query.setRequestingClient(d[3]);
                    query.setAnswerType(AnswerType.getByMapping(Integer.parseInt(d[4])));

                    return query;
                })
                .sorted(Comparator.comparing(PiHoleQuery::getDate).reversed())
                .collect(Collectors.toList());


    }

    public String getUrl() {
        return url;
    }


    private class PiHoleResults {
        private String[][] data;

        public String[][] getData() {
            return data;
        }

        public void setData(String[][] data) {
            this.data = data;
        }
    }
}
