package com.ftpix.homedash.plugins.mma;

import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.notifications.Notifications;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.sherdogparser.Sherdog;
import com.ftpix.sherdogparser.models.Event;
import com.ftpix.sherdogparser.models.Organization;
import com.mashape.unirest.http.Unirest;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by gz on 21-Aug-16.
 */
public class MmaPlugin extends Plugin {
    private Organization organization;

    private final String ORGANIZATION_URL = "url", NOTIFICATION_SETTING = "notifications";
    private final String COMMAND_GET_EVENT = "getEvent", COMMAND_GET_FIGHTER = "getFighter";
    private String organizationUrl;
    private boolean sendNotifications;
    private Sherdog sherdog;
    //keeping the hserdog url of last event so we can avoid sending notification more than once
    private String lastNotificationSent = "";

    @Override
    public String getId() {
        return "mma";
    }

    @Override
    public String getDisplayName() {
        return "Mixed Martial Arts events";
    }

    @Override
    public String getDescription() {
        return "Show the upcoming event of a MMA organiztion and allow you to browse the database for events, fights and fighters";
    }

    @Override
    public String getExternalLink() {
        return null;
    }

    @Override
    protected void init() {
        organizationUrl = settings.get(ORGANIZATION_URL);
        sendNotifications = settings.containsKey(NOTIFICATION_SETTING);

        sherdog = new Sherdog.Builder().withCacheFolder(getCacheFolder()).build();
    }

    @Override
    public String[] getSizes() {
        return new String[]{"3x4", "4x4"};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return ONE_HOUR;
    }

    @Override
    protected WebSocketMessage processCommand(String command, String message, Object extra) {
        WebSocketMessage response = new WebSocketMessage();
        try {
            switch (command) {
                case COMMAND_GET_EVENT:
                    response.setCommand(COMMAND_GET_EVENT);
                    response.setMessage(sherdog.getEvent(message));
                    break;
                case COMMAND_GET_FIGHTER:
                    response.setCommand(COMMAND_GET_FIGHTER);
                    response.setMessage(sherdog.getFighter(message));
            }
        } catch (Exception e) {
            logger.error("Error while getting event or fight", e);
            response.setCommand(WebSocketMessage.COMMAND_ERROR);
            response.setMessage(e.getMessage());
        }
        return response;
    }

    @Override
    public void doInBackground() {
        logger.info("Getting organization...");
        try {
            organization = sherdog.getOrganization(organizationUrl);
            ZonedDateTime today = ZonedDateTime.now();
            today.minus(1, ChronoUnit.DAYS);
            logger.info("Found {} with [{}] events", organization.getName(), organization.getEvents().size());

            organization.setEvents(organization.getEvents().stream().filter(e -> today.isBefore(e.getDate())).collect(Collectors.toList()));
            logger.info("Found {} with [{}] upcoming events", organization.getName(), organization.getEvents().size());

            //Checking if we need to send notification
            if (sendNotifications && !organization.getEvents().isEmpty()) {
                LocalDate todayDate = LocalDate.now();

                organization.getEvents().stream()
                        .filter(e -> !e.getSherdogUrl().equalsIgnoreCase(lastNotificationSent) && LocalDate.from(e.getDate()).equals(todayDate))
                        .findFirst()
                        .ifPresent(e -> {
                            String title = e.getName() + " is happening today !";
                            StringBuilder sb = new StringBuilder("Fights \n");

                            try {
                                Event event = sherdog.getEvent(e.getSherdogUrl());

                                event.getFights().forEach(f -> {
                                    sb.append(f.getFighter1().getName()).append(" vs ").append(f.getFighter2().getName()).append("\n");
                                });

                            } catch (IOException e1) {
                                e1.printStackTrace();
                            } catch (ParseException e1) {
                                e1.printStackTrace();
                            }

                            lastNotificationSent = e.getSherdogUrl();

                            Notifications.send(title, sb.toString());
                        });
            }
        } catch (Exception e) {
            logger.error("Error while trying to get the organization");
        }
    }

    @Override
    protected Object refresh(String size) throws Exception {
        return organization;
    }

    @Override
    public int getRefreshRate() {
        return ONE_HOUR;
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> errors = new HashMap<>();

        if (settings.get(ORGANIZATION_URL).trim().isEmpty()) {
            errors.put("Url", "Organization URL can't be empty");
        } else {
            try {
                Unirest.get(settings.get(ORGANIZATION_URL)).asString().getBody();
            } catch (Exception e) {
                errors.put("Url", "Can't reach the given url");
            }
        }

        return errors;
    }

    @Override
    public ModuleExposedData exposeData() {
        ModuleExposedData exposed = new ModuleExposedData();
        if (organization.getEvents().size() > 0) {
            exposed.addText("Next event");
            Event upcoming = organization.getEvents().get(organization.getEvents().size() - 1);
            exposed.addText(upcoming.getName());
            exposed.addText(upcoming.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        } else {
            exposed.addText("No upcoming event");
        }
        exposed.setModuleName(getDisplayName());
        return null;
    }

    @Override
    public Map<String, String> exposeSettings() {
        Map<String, String> settings = new HashMap<>();
        settings.put("Sherdog url", organizationUrl);

        return settings;
    }
}
