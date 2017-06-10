package com.ftpix.homedash.plugins.googlepubliccalendar;

import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by gz on 24-Jul-16.
 */
public class GooglePublicCalendarPlugin extends Plugin {

    private final String CALENDAR_ID = "calendarId", API_KEY = "apiKey";
    private String calendarId, apiKey, timeZone = TimeZone.getDefault().getID();
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    private SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    // private SimpleDateFormat df3 = new SimpleDateFormat("yyyy-MM-dd");
    private final String URL = "https://www.googleapis.com/calendar/v3/calendars/[ID]/events?orderBy=startTime&singleEvents=true&timeMin=[STARTTIME]&timeZone=[TIMEZONE]&key=[APIKEY]";
    private String url = "";


    @Override
    public String getId() {
        return "googlepubliccalendar";
    }

    @Override
    public String getDisplayName() {
        return "Google Public Calendar";
    }

    @Override
    public String getDescription() {
        return "View upcoming events from any Google public calendar.";
    }

    @Override
    public String getExternalLink() {
        return null;
    }

    @Override
    protected void init() {
        try {
            apiKey = settings.get(API_KEY);
            calendarId = settings.get(CALENDAR_ID);
            url = URL.replace("[ID]", URLEncoder.encode(calendarId, "UTF-8")).replace("[APIKEY]", apiKey).replace("[TIMEZONE]", URLEncoder.encode(timeZone, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            logger.error("Error while encoding calendar id", e);
        }
    }

    @Override
    public String[] getSizes() {
        return new String[]{"3x1", "3x4", "4x4", ModuleLayout.FULL_SCREEN};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return 0;
    }

    @Override
    protected WebSocketMessage processCommand(String command, String message, Object extra) {
        return null;
    }

    @Override
    public void doInBackground() {

    }

    @Override
    protected Object refresh(String size) throws Exception {

        try {

            return getCalendar();

        } catch (Exception e) {
            logger.info("Error while parsing calendar");
        }

        return null;
    }

    @Override
    public int getRefreshRate(String size) {
        return ONE_HOUR;
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        try {
            String apiKey = settings.get(API_KEY);
            String calendarId = settings.get(CALENDAR_ID);
            String url = URL.replace("[ID]", URLEncoder.encode(calendarId, "UTF-8")).replace("[APIKEY]", apiKey).replace("[TIMEZONE]", URLEncoder.encode(timeZone, "UTF-8"));

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            url = url.replace("[STARTTIME]", URLEncoder.encode(df.format(today.getTime()), "UTF-8"));

            Unirest.get(url).asString().getBody().toString();
        } catch (Exception e) {
            Map<String, String> result = new HashMap<>();
            result.put("Calendar not available", "The calandar you set is not reachable for the following reason '" + e.getMessage() + "'");
            return result;
        }
        return null;
    }

    @Override
    public ModuleExposedData exposeData() {
        try {
            ModuleExposedData data = new ModuleExposedData();
            GoogleCalendar calendar;
            calendar = getCalendar();
            if (calendar.events.size() > 0) {
                GoogleCalendarEvent event = calendar.events.get(0);
                data.addText(calendar.title);
                data.addText(event.startTime);
                data.addText(event.summary);

                return data;
            }

            return null;
        } catch (Exception e) {
            logger.error("Couldn't expose google calendar data", e);
            return null;
        }
    }

    @Override
    public Map<String, String> exposeSettings() {
        Map<String, String> result = new HashMap<>();

        try {
            String apiKey = settings.get(API_KEY);
            String calendarId = settings.get(CALENDAR_ID);
            String url = URL.replace("[ID]", URLEncoder.encode(calendarId, "UTF-8")).replace("[APIKEY]", apiKey).replace("[TIMEZONE]", URLEncoder.encode(timeZone, "UTF-8"));

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            url = url.replace("[STARTTIME]", URLEncoder.encode(df.format(today.getTime()), "UTF-8"));

            try {
                JSONObject json = new JSONObject(Unirest.get(url).asString().getBody().toString());

                result.put("Calendar", StringEscapeUtils.escapeHtml4(json.getString("summary")));

            } catch (Exception e) {
                logger.error("Can't expose settings for goolge calendar", e);
            }

            return result;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    @Override
    protected void onFirstClientConnect() {

    }

    @Override
    protected void onLastClientDisconnect() {

    }

    @Override
    protected Map<String, Object> getSettingsModel() {
        return null;
    }

    /// plugin methods
    private GoogleCalendar getCalendar() throws JSONException, IOException, ParseException, UnirestException {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);

        String url = this.url.replace("[STARTTIME]", URLEncoder.encode(df.format(today.getTime()), "UTF-8"));

        JSONObject json = new JSONObject(Unirest.get(url).asString().getBody().toString());
        GoogleCalendar calendar = new GoogleCalendar();
        calendar.title = StringEscapeUtils.escapeHtml4(json.getString("summary"));
        calendar.description = StringEscapeUtils.escapeHtml4(json.getString("description"));

        JSONArray jsonarray = json.getJSONArray("items");

        for (int i = 0; i < jsonarray.length(); i++) {

            JSONObject jsonEvent = jsonarray.getJSONObject(i);

            GoogleCalendarEvent event = new GoogleCalendarEvent();
            try {
                event.summary = StringEscapeUtils.escapeHtml4(jsonEvent.getString("summary"));
            } catch (Exception e) {
            }

            try {
                event.description = StringEscapeUtils.escapeHtml4(jsonEvent.getString("description"));
            } catch (Exception e) {
            }

            try {
                event.link = jsonEvent.getString("htmlLink");
            } catch (Exception e) {
            }
            try {
                Date date = df.parse(jsonEvent.getJSONObject("start").getString("dateTime"));
                event.startTime = df2.format(date);
            } catch (JSONException e) {
                event.startTime = jsonEvent.getJSONObject("start").getString("date");
            }

            calendar.events.add(event);

        }

        return calendar;
    }

    //////inner classes
    private class GoogleCalendarEvent {
        public String summary, description, startTime, link;
    }

    private class GoogleCalendar {
        public String title, description;
        public List<GoogleCalendarEvent> events = new ArrayList<GoogleCalendarEvent>();
    }
}
