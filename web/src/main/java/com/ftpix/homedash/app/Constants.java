package com.ftpix.homedash.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;
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
    public static boolean STATIC_CONFIG = false;

    static {


        DEV_MODE = Boolean.parseBoolean(System.getProperty("dev", "false"));

        Properties prop = new Properties();

        logger.info("Loading conf file");
        try {

            String configLocation = System.getProperty("config.file", null);
            if (configLocation != null) {
                File file = new File(configLocation);
//                URL[] urls = {file.getParentFile().toURI().toURL()};
//                ClassLoader loader = new URLClassLoader(urls);
//                rs = ResourceBundle.getBundle(file.getName().replace(".properties", ""), Locale.getDefault(), loader);
                prop.load(new FileInputStream(file));

                logger.info("Loading external config file: {}", file.getAbsolutePath());
            } else {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                try (InputStream resourceStream = loader.getResourceAsStream("homedash.properties")) {
                    prop.load(resourceStream);
                }
            }
        } catch (Exception e) {
            logger.info("couldn't find file homedash.properties, checking if the default file exists");

            logger.error("Please copy homedash.properties.default to homedash.properties", e);

            System.exit(-1);
        }

        String path = prop.getProperty("cache_path");
        if (!path.endsWith("/")) {
            path += "/";
        }


        boolean secure = false;
        try {
            if (Boolean.valueOf(prop.getProperty("secure"))) {
                secure = true;
            }
        } catch (Exception e) {
            logger.info("Can't fins secure parameter, assuming false");
        }


        SECURE = secure;
        if (SECURE) {
            KEY_STORE = prop.getProperty("key_store");
            KEY_STORE_PASS = prop.getProperty("key_store_pass");
        } else {
            KEY_STORE_PASS = null;
            KEY_STORE = null;
        }


        CACHE_FOLDER = path;

        DB_PATH = prop.getProperty("db_path");

        PORT = Integer.parseInt(prop.getProperty("port"));

        File f = new File(CACHE_FOLDER);
        if (!f.exists()) {
            f.mkdirs();
        }

        SALT = prop.getProperty("salt");

        logger.info("DB_PATH:{}", DB_PATH);
        logger.info("Cache folder:{}", CACHE_FOLDER);
        logger.info("Port: {}", PORT);
    }
}
