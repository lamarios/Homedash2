package com.ftpix.homedash.notifications;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class Notifications {
    private static final Logger logger = LogManager.getLogger();

    private static final Set<NotificationProvider> registered = new HashSet<>();

    public static void send(String title, String body) {
        send(title, body, false);
    }

    /**
     * Send a notification to all the registerd providers
     * @param title
     * @param body
     */
    public static void send(String title, String body, boolean test) {
        logger.info("Sending notification to [{}] providers: title:[{}] body:[{}]", registered.size(), title, body);
        registered.forEach(provider ->{
            try {
                logger.info("to provider [{}]", provider.getName());
                provider.sendNotification(title, body);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Add a new provider (only if it doesn't already exists)
     * @param provider
     */
    public static void registerProvider(NotificationProvider provider){
        registered.add(provider);
        logger.info("Registering provider {}, Total providers: [{}]" ,provider.getName(), registered.size());
    }

    public static void resetRegisteredProvider(){
        logger.info("Clearing all the providers");
        registered.clear();
    }


}
