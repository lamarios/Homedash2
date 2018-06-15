package com.ftpix.homedash.app;

import com.ftpix.homedash.app.controllers.SettingsController;
import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.jobs.BackgroundRefresh;
import com.ftpix.homedash.models.Layout;
import com.ftpix.homedash.models.Page;
import com.ftpix.homedash.plugins.SystemInfoPlugin;
import com.ftpix.homedash.websocket.FullScreenWebSocket;
import com.ftpix.homedash.websocket.SingleModuleKioskWebSocket;
import com.ftpix.homedash.websocket.MainWebSocket;
import com.google.common.io.Files;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DateBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static spark.Spark.before;
import static spark.Spark.port;
import static spark.Spark.staticFileLocation;
import static spark.Spark.webSocket;

/**
 * Hello world!
 */
public class App {
    private static Logger logger = LogManager.getLogger();
    private final static String NATIVE_LIBS_FOLDER_NAME = "native-libs";

    public static void main(String[] args) {
        try {

            URL resource = App.class.getResource("/");

            loadNativeLibs();

//            staticFileLocation("/web");

            port(Constants.PORT);

            webSocket("/ws", MainWebSocket.class);
            webSocket("/ws-full-screen", FullScreenWebSocket.class);
            webSocket("/ws-kiosk", SingleModuleKioskWebSocket.class);

            //No cache policy, especially against Edge and IE
            before((req, res) -> {
                res.header("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
                res.header("Pragma", "no-cache"); // HTTP 1.0.
                res.header("Expires", "0"); // Proxies.
                logger.info("{} -> {}", req.requestMethod(), req.url());
                if (!req.pathInfo().startsWith("/api") && !req.pathInfo().startsWith("/cache") && !req.pathInfo().equalsIgnoreCase("/login") && !SettingsController.INSTANCE.checkSession(req, res)) {
                    res.redirect("/login");
                }
            });

            createDefaultData();
            Endpoints.define();


            // set up the notifications
            SettingsController.INSTANCE.updateNotificationProviders();

//            enableDebugScreen();

            prepareJobs();

        } catch (Exception e) {
            logger.error("Error during startup, we better stop everything", e);
            System.exit(1);
        }
    }


    /**
     * Load all the native libs from other modules
     */
    private static void loadNativeLibs() throws Exception {
        logger.info("Loading native libs if any");
        Properties props = System.getProperties();

        File dir = null;

        final File jarFile = new File(App.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        if (jarFile.isFile()) {  // Run with JAR file
            dir = Files.createTempDir();
            logger.info("Created tmp directory [{}]", dir.getAbsolutePath());
            dir.mkdir();
            final String path = NATIVE_LIBS_FOLDER_NAME;
            final JarFile jar = new JarFile(jarFile);
            final Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                final String name = jarEntry.getName();

                if (name.startsWith(path + "/") && !jarEntry.isDirectory()) { //filter according to the path

                    File toCopy = new File(dir.getAbsolutePath() + File.separator + name);
                    if (!toCopy.exists()) {
                        try {
                            logger.info("Copying [{}] to [{}]", name, toCopy.getAbsolutePath());

                            if (!toCopy.getParentFile().exists()) {
                                toCopy.getParentFile().mkdir();
                            }
                            InputStream is = jar.getInputStream(jarEntry); // get the input stream
                            FileOutputStream fos = new FileOutputStream(toCopy);
                            while (is.available() > 0) {  // write contents of 'is' to 'fos'
                                fos.write(is.read());
                            }
                            fos.close();
                            is.close();
                        } catch (Exception e) {
                            logger.error("Error while extracting [" + name + "] continuing but it may cause issues", e);
                        }
                    }
                }
            }
            jar.close();

            dir = new File(dir.getAbsolutePath() + File.separator + NATIVE_LIBS_FOLDER_NAME);
            logger.info("Set lib path [{}]", dir.getAbsolutePath());

        } else {// run in IDE

            URL url = SystemInfoPlugin.class.getClassLoader().getResource(NATIVE_LIBS_FOLDER_NAME);
            if (url != null) {
                dir = new File(url.toURI());
            }

        }

        if (dir != null) {
            logger.info("Setting [{}] as library path", dir.getAbsolutePath());
            props.setProperty("java.library.path", dir.getAbsolutePath());
        }

    }

    /**
     * Create default data like layouts and the main page
     */
    public static void createDefaultData() throws SQLException {
        logger.info("Creating first page if it doesn't exist");
        Page page = new Page();
        page.setId(1);
        page.setName("Main");

        DB.PAGE_DAO.createIfNotExists(page);

        logger.info("Creating the 3 default layouts");
        Layout desktop = new Layout();
        desktop.setId(1);
        desktop.setMaxGridWidth(11);
        desktop.setName("Desktop");

        DB.LAYOUT_DAO.createOrUpdate(desktop);

        Layout tablet = new Layout();
        tablet.setId(2);
        tablet.setMaxGridWidth(8);
        tablet.setName("Tablet");

        DB.LAYOUT_DAO.createOrUpdate(tablet);

        Layout mobile = new Layout();
        mobile.setId(3);
        mobile.setMaxGridWidth(3);
        mobile.setName("Mobile");

        DB.LAYOUT_DAO.createOrUpdate(mobile);

    }

    /**
     * Create the scheduling jobs for refreshing the modules in the background
     */
    private static void prepareJobs() throws SchedulerException {
        SchedulerFactory sf = new StdSchedulerFactory();
        Scheduler scheduler = sf.getScheduler();

        Date runTime = DateBuilder.evenMinuteDate(new Date());

        JobDetail job = JobBuilder.newJob(BackgroundRefresh.class).withIdentity("BackgroundRefresh", "HomeDash").build();

        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("BackgroundRefresh", "HomeDash").withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(1).repeatForever())
                .build();

        scheduler.scheduleJob(job, trigger);
        logger.info(job.getKey() + " will run at: " + runTime);

        scheduler.start();
    }
}
