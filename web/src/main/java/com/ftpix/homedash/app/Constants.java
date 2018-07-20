package com.ftpix.homedash.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Constants {
    public final static String CACHE_FOLDER;
    public final static String DB_PATH;
    public final static int PORT;
    public final static String SALT;
    private static Logger logger = LogManager.getLogger();
    public final static boolean DEV_MODE;
    public final static boolean SECURE;
    public static final String KEY_STORE, KEY_STORE_PASS;

    static {


        DEV_MODE = Boolean.parseBoolean(System.getProperty("dev", "false"));

        logger.info("Loading conf file");
        ResourceBundle rs = null;
        try {
            rs = ResourceBundle.getBundle("homedash");
        } catch (Exception e) {
            logger.info("couldn't find file homedash.properties, checking if the default file exists");

            logger.info("Please copy homedash.properties.default to homedash.properties");
            System.exit(-1);
        }

        String path = rs.getString("cache_path");
        if (!path.endsWith("/")) {
            path += "/";
        }


        boolean secure = false;
        try {
            if (Boolean.valueOf(rs.getString("secure"))) {
                secure = true;
            }
        } catch (Exception e) {
            logger.info("Can't fins secure parameter, assuming false");
        }


        SECURE = secure;
        if (SECURE) {
            KEY_STORE = rs.getString("key_store");
            KEY_STORE_PASS = rs.getString("key_store_pass");
        } else {
            KEY_STORE_PASS = null;
            KEY_STORE = null;
        }


        CACHE_FOLDER = path;

        DB_PATH = rs.getString("db_path");

        PORT = Integer.parseInt(rs.getString("port"));

        File f = new File(CACHE_FOLDER);
        if (!f.exists()) {
            f.mkdirs();
        }

        SALT = rs.getString("salt");

        logger.info("DB_PATH:{}", DB_PATH);
        logger.info("Cache folder:{}", CACHE_FOLDER);
        logger.info("Port", PORT);
    }
}
