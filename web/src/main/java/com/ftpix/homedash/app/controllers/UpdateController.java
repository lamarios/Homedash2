package com.ftpix.homedash.app.controllers;

import com.ftpix.homedash.updater.Updater;
import com.ftpix.homedash.updater.exceptions.WrongVersionPatternException;
import com.google.gson.Gson;
import io.gsonfire.GsonFireBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by gz on 4/1/17.
 */
public class UpdateController {
  private static UpdateController instance = new UpdateController();
  private final Gson gson = new GsonFireBuilder().enableExposeMethodResult().createGsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
  private final Logger logger = LogManager.getLogger();
  private Updater updater = new Updater();

  public static UpdateController getInstance() {
    return instance;
  }


  /**
   * Defines all the endpoints used by this controller
   */
  public void defineEndpoints() {
    Spark.get("/updater/check-version", "application/json", this::getVersion, gson::toJson);
    Spark.get("/updater/auto-update", "application/json", this::update, gson::toJson);
  }


  /**
   * the auto update process
   *
   * @param request
   * @param response
   * @return
   */
  private String update(Request request, Response response) {

    ExecutorService exec = Executors.newSingleThreadExecutor();
    exec.execute(() -> {
      try {
        logger.info("Downloading last version");
        File latestVersion = updater.downloadLatestVersion();
        logger.info("Latest version downloaded : [{}]", latestVersion.getAbsolutePath());
        //Stopping the server so we can update peacefully
        if (updater.isFolderStructureOkForAutoUpdate()) {
          Spark.stop();
          try {
            updater.stopHomedashAndTriggerUpdate(latestVersion.toPath().toAbsolutePath());
          } catch (Exception e) {
            logger.error("error while updating, restarting Spark", e);
            Spark.init();
          }
        }
      } catch (Exception e) {
        logger.error("Error while downloading the latest version", e);
      }

    });


    return "Homedash is updating and will restart on its own, try to stay still.";
  }


  /**
   * Logic to get the latest version and to know if an auto update is possible
   *
   * @param req
   * @param res
   * @return
   * @throws IOException
   */
  private Map<String, Object> getVersion(Request req, Response res) throws IOException {
    return getVersion();
  }

  /**
   * Gets the current and check for version.
   *
   * @return a map with all the necessary data.
   * @throws IOException
   */
  public Map<String, Object> getVersion() {
    Map<String, Object> status = new HashMap<>();
    try {

      String current = updater.getCurrentVersion();
      String latest = updater.getLatestVersion().getName();
      status.put("current", current);
      status.put("latest", latest);

      try {
        boolean canUpdate = updater.compareVersion(current, latest) == -1;
        if (canUpdate) {
          canUpdate = updater.isFolderStructureOkForAutoUpdate();
          if (canUpdate) {
            status.put("canUpdate", true);
            status.put("message", "");
          } else {
            status.put("canUpdate", false);
            status.put("message", "You're not running HomeDash from the distribution version, you need to update Homedash manually");
          }
        } else {
          status.put("canUpdate", false);
          status.put("message", "You're already running the latest version");
        }

      } catch (WrongVersionPatternException e) {
        status.put("canUpdate", false);
        status.put("message", "The error versions don't match the pattern x.x.x");
      }
    } catch (IOException | NullPointerException e) {
      status.put("canUpdate", false);
      status.put("message", "Error getting latest version: " + e.getMessage());
    }
    return status;
  }

}
