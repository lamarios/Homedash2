package com.ftpix.homedash.app;

import java.io.File;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



public class Constants {
	public final static String CACHE_FOLDER;
	public final static String DB_PATH;
	public final static int PORT;

	private static Logger logger = LogManager.getLogger();

	static {
		
		logger.info("Loading conf file");
		ResourceBundle rs = ResourceBundle.getBundle("conf/homedash");

		String path = rs.getString("cache_path");
		;
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
		
		
		logger.info("DB_PATH:{}", DB_PATH);
		logger.info("Cache folder:{}", CACHE_FOLDER);
		logger.info("Port", PORT);
	}
}
