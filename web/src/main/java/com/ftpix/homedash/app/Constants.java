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

    static {

        logger.info("Loading conf file");
        ResourceBundle rs = null;
        try {
            rs = ResourceBundle.getBundle("homedash");
        }catch (Exception e){
            logger.info("couldn't find file homedash.properties, checking if the default file exists");

            logger.info("Please copy homedash.properties.default to homedash.properties");
            System.exit(-1);
        }

        String path = rs.getString("cache_path");
        if (!path.endsWith("/")) {
            path += "/";
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
