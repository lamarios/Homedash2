package com.ftpix.homedash.plugins.mma;

import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.notifications.Notifications;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.plugins.mma.model.HomeDashEvent;
import com.ftpix.homedash.plugins.mma.model.HomeDashOrganization;
import com.ftpix.sherdogparser.PictureProcessor;
import com.ftpix.sherdogparser.Sherdog;
import com.ftpix.sherdogparser.exceptions.SherdogParserException;
import com.ftpix.sherdogparser.models.Event;
import com.ftpix.sherdogparser.models.Fight;
import com.ftpix.sherdogparser.models.Fighter;
import com.ftpix.sherdogparser.models.Organization;
import com.ftpix.sherdogparser.parsers.ParserUtils;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by gz on 21-Aug-16.
 */
public class MmaPlugin extends Plugin {
    private HomeDashOrganization organization;

    private final String ORGANIZATION_URL = "url", NOTIFICATION_SETTING = "notifications";
    private final String COMMAND_GET_EVENT = "getEvent", COMMAND_GET_FIGHTER = "getFighter", COMMAND_SEARCH = "search";
    private String organizationUrl, searchQuery = "";
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

        PictureProcessor processor = (url, fighter) -> {

            Path p = getCacheFolder().resolve(DigestUtils.md5Hex(fighter.getSherdogUrl()) + ".jpg");
            ParserUtils.downloadImageToFile(url, p);

            return p.toString();
        };

        sherdog = new Sherdog.Builder().withPictureProcessor(processor).build();
    }


    @Override
    public int getBackgroundRefreshRate() {
        return ONE_HOUR * 24;
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
                    Fighter fighter = sherdog.getFighter(message);
                    fighter.setPicture("/" + fighter.getPicture());
                    response.setMessage(fighter);
                    break;
                case COMMAND_SEARCH:
                    this.searchQuery = message;
                    break;
            }
        } catch (Exception e) {
            logger().error("Error while getting event or fight", e);
            response.setCommand(WebSocketMessage.COMMAND_ERROR);
            response.setMessage(e.getMessage());
        }
        return response;
    }

    @Override
    public void doInBackground() {
        logger().info("Getting organization...");
        try {
            Organization org = sherdog.getOrganization(organizationUrl);
            organization = new HomeDashOrganization(org);


            logger().info("Found {} with [{}] events", organization.getName(), organization.getEvents().size());


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

                            } catch (IOException | SherdogParserException | ParseException e1) {
                                e1.printStackTrace();
                            }

                            lastNotificationSent = e.getSherdogUrl();

                            Notifications.send(title, sb.toString());
                        });


            }

            ZonedDateTime today = ZonedDateTime.now().minus(1, ChronoUnit.DAYS);

            organization.setHomeDashEvents(org.getEvents().parallelStream().map(
                    event -> {
                        if (event.getDate().isAfter(today)) {
                            try {
                                Event fullEvent = sherdog.getEvent(event.getSherdogUrl());
                                HomeDashEvent hdEvent = new HomeDashEvent(event);
                                if (fullEvent.getFights().size() > 0) {
                                    Fight fight = fullEvent.getFights().get(0);
                                    hdEvent.setMainEventPhoto1("/" + sherdog.getFighter(fight.getFighter1().getSherdogUrl()).getPicture());
                                    hdEvent.setMainEventPhoto2("/" + sherdog.getFighter(fight.getFighter2().getSherdogUrl()).getPicture());
                                    logger().info("Main event pictures {}, {}", hdEvent.getMainEventPhoto1(), hdEvent.getMainEventPhoto2());
                                }
                                return hdEvent;
                            } catch (Exception e) {
                                logger().error("Couldn't retrieve fighter picture", e);

                            }
                        }

                        return new HomeDashEvent(event);
                    }).collect(Collectors.toList()));
        } catch (Exception e) {
            logger().error("Error while trying to get the organization");
        }
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    protected Object refresh(boolean fullScreen) throws Exception {
        if (!fullScreen) {
            ZonedDateTime today = ZonedDateTime.now().minus(1, ChronoUnit.DAYS);

            return search(e -> today.isBefore(e.getDate()));
        } else {
            return search(e -> e.getName().contains(searchQuery));
        }
    }

    @Override
    public int getRefreshRate(boolean fullScreen) {
        if (fullScreen) {
            return ONE_SECOND * 3;
        } else {
            return ONE_HOUR;
        }
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> errors = new HashMap<>();

        if (settings.get(ORGANIZATION_URL).trim().isEmpty()) {
            errors.put("Url", "Organization URL can't be empty");
        } else {
            try {

                Organization o = new Sherdog().getOrganization(settings.get(ORGANIZATION_URL));
                if (o == null) {
                    errors.put("Url", "Organization doesn't exist");
                }
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

    @Override
    public boolean hasFullScreen() {
        return true;
    }

    private Organization search(Predicate<Event> filter) {
        HomeDashOrganization org = new HomeDashOrganization(organization);
        org.setHomeDashEvents((organization.getHomeDashEvents()).stream()
                .filter(filter)
                .collect(Collectors.toList()));
        return org;
    }
}
