package com.ftpix.homedash.db;

import com.ftpix.homedash.app.Constants;
import com.ftpix.homedash.db.schemaManagement.UpdateStep;
import com.ftpix.homedash.db.schemaManagement.updates.Update20170722;
import com.ftpix.homedash.db.schemaManagement.updates.Update20200710;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.*;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class DB {
    private final static String databaseUrl = "jdbc:h2:" + Constants.DB_PATH;
    public static Dao<Module, Integer> MODULE_DAO = null;
    public static Dao<Page, Integer> PAGE_DAO = null;
    public static Dao<ModuleSettings, Integer> MODULE_SETTINGS_DAO = null;
    public static Dao<Settings, String> SETTINGS_DAO = null;
    public static Dao<ModuleData, Integer> MODULE_DATA_DAO = null;
    public static Dao<RemoteFavorite, Integer> REMOTE_FAVORITE_DAO = null;
    private static Dao<Schema, Integer> SCHEMA_DAO = null;
    private static Logger logger = LogManager.getLogger();

    private static ConnectionSource connectionSource;

    static {

        try {

            logger.info("Initiating DB and DAO to DB:[{}]", databaseUrl);

            // this uses h2 by default but change to match your database
            // create a connection source to our database
            connectionSource = new JdbcConnectionSource(databaseUrl, "sa", "");

            logger.info("Creating Module DAO and tables if it doesn't exist");
            MODULE_DAO = DaoManager.createDao(connectionSource, Module.class);
            TableUtils.createTableIfNotExists(connectionSource, Module.class);

            logger.info("Creating Page DAO and tables if it doesn't exist");
            PAGE_DAO = DaoManager.createDao(connectionSource, Page.class);
            TableUtils.createTableIfNotExists(connectionSource, Page.class);

//            logger.info("Creating Layout DAO and tables if it doesn't exist");
//            LAYOUT_DAO = DaoManager.createDao(connectionSource, Layout.class);
//            TableUtils.createTableIfNotExists(connectionSource, Layout.class);
//
//            logger.info("Creating Module Layout DAO and tables if it doesn't exist");
//            MODULE_LAYOUT_DAO = DaoManager.createDao(connectionSource, ModuleLayout.class);
//            TableUtils.createTableIfNotExists(connectionSource, ModuleLayout.class);

            logger.info("Creating Module Settings DAO and tables if it doesn't exist");
            MODULE_SETTINGS_DAO = DaoManager.createDao(connectionSource, ModuleSettings.class);
            TableUtils.createTableIfNotExists(connectionSource, ModuleSettings.class);

            logger.info("Creating Module Data DAO and tables if it doesn't exist");
            MODULE_DATA_DAO = DaoManager.createDao(connectionSource, ModuleData.class);
            TableUtils.createTableIfNotExists(connectionSource, ModuleData.class);


            logger.info("Creating  Settings DAO and tables if it doesn't exist");
            SETTINGS_DAO = DaoManager.createDao(connectionSource, Settings.class);
            TableUtils.createTableIfNotExists(connectionSource, Settings.class);


            logger.info("Creating  remote favorite DAO and tables if it doesn't exist");
            REMOTE_FAVORITE_DAO = DaoManager.createDao(connectionSource, RemoteFavorite.class);
            TableUtils.createTableIfNotExists(connectionSource, RemoteFavorite.class);

            logger.info("Creating Schema DAO and tables if it doesn't exist");
            SCHEMA_DAO = DaoManager.createDao(connectionSource, Schema.class);
            TableUtils.createTableIfNotExists(connectionSource, Schema.class);

            updateSchema(connectionSource);

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            logger.info("Error while setting up ORM", e);
            System.exit(0);
        }
    }


    public static boolean clearTable(Class dataClass) throws SQLException {
        return TableUtils.clearTable(connectionSource, dataClass) >= 0;
    }


    private static void updateSchema(ConnectionSource connectionSource) throws SQLException {
        //getting current version
        Version version = Optional.ofNullable(DB.SCHEMA_DAO.queryForAll())
                .filter(list -> list.size() > 0)
                .map(list -> list.get(0))
                .map(sch -> sch.getVersion())
                .map(sch -> new Version(sch))
                .orElse(new Version("0.0.0"));

        logger.info("Current version: [{}]", version.toString());

        List.of(Update20170722.class, Update20200710.class)
                .stream()
                .map(step -> {
                    try {
                        return (UpdateStep) step.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        logger.info("Couldn't instanciate class {}", step.getCanonicalName());
                        return null;
                    }
                })
                .filter(step -> step != null)
                //getting only the ones with more recent version
                .filter(step -> version.compareTo(step.getVersion()) == -1)
                //sorting it by version numbers
                .sorted(Comparator.comparing(UpdateStep::getVersion))
                .forEach(step -> {
                    logger.info("Executing step for version [{}]", step.getVersion());
                    //running the queries
                    step.ups().forEach(up -> {
                        try {
                            logger.info("Executing sql [{}]", up);
                            DB.SCHEMA_DAO.executeRaw(up);
                        } catch (SQLException e) {
                            logger.error("[{}] Couldn't execute query: {}", step.getVersion(), up, e);
                            throw new RuntimeException(e);
                        }
                    });

                    //inserting version if everything went ok
                    try {
                        logger.info("Inserting version [{}] to schema", step.getVersion());
                        TableUtils.clearTable(connectionSource, Schema.class);
                        Schema schema = new Schema();
                        schema.setVersion(step.getVersion().toString());
                        SCHEMA_DAO.create(schema);
                    } catch (SQLException e) {
                        logger.error("Error while setting new schema version to {}", step.getVersion(), e);
                        throw new RuntimeException(e);
                    }


                });

        //running the queries

    }
}
