package com.ftpix.homedash.db;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ftpix.homedash.app.Constants;
import com.ftpix.homedash.models.Layout;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.ModuleSettings;
import com.ftpix.homedash.models.Page;
import com.ftpix.homedash.models.Settings;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class DB {
	public static Dao<Module, Integer> MODULE_DAO = null;
	public static Dao<Page, Integer> PAGE_DAO = null;
	public static Dao<Layout, Integer> LAYOUT_DAO = null;
	public static Dao<ModuleLayout, Integer> MODULE_LAYOUT_DAO = null;
	public static Dao<ModuleSettings, Integer> MODULE_SETTINGS_DAO = null;
	public static Dao<Settings, String> SETTINGS_DAO = null;
	
	private final static String databaseUrl = "jdbc:h2:" + Constants.DB_PATH;

	private static Logger logger = LogManager.getLogger();

	static {

		try {

			logger.info("Initiating DB and DAO to DB:[{}]", databaseUrl);

			// this uses h2 by default but change to match your database
			// create a connection source to our database
			ConnectionSource connectionSource = new JdbcConnectionSource(databaseUrl, "sa", "");

			logger.info("Creating Module DAO and tables if it doesn't exist");
			MODULE_DAO = DaoManager.createDao(connectionSource, Module.class);
			TableUtils.createTableIfNotExists(connectionSource, Module.class);

			logger.info("Creating Page DAO and tables if it doesn't exist");
			PAGE_DAO = DaoManager.createDao(connectionSource, Page.class);
			TableUtils.createTableIfNotExists(connectionSource, Page.class);
			
			logger.info("Creating Layout DAO and tables if it doesn't exist");
			LAYOUT_DAO = DaoManager.createDao(connectionSource, Layout.class);
			TableUtils.createTableIfNotExists(connectionSource, Layout.class);
			
			logger.info("Creating Module Layout DAO and tables if it doesn't exist");
			MODULE_LAYOUT_DAO = DaoManager.createDao(connectionSource, ModuleLayout.class);
			TableUtils.createTableIfNotExists(connectionSource, ModuleLayout.class);
			
			logger.info("Creating Module Settings DAO and tables if it doesn't exist");
			MODULE_SETTINGS_DAO = DaoManager.createDao(connectionSource, ModuleSettings.class);
			TableUtils.createTableIfNotExists(connectionSource, ModuleSettings.class);
			
			
			logger.info("Creating  Settings DAO and tables if it doesn't exist");
			SETTINGS_DAO = DaoManager.createDao(connectionSource, Settings.class);
			TableUtils.createTableIfNotExists(connectionSource, Settings.class);

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}
	}
}
