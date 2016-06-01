package com.ftpix.homedash.app.controllers;


import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.Settings;
import spark.ModelAndView;
import spark.template.jade.JadeTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.get;
import static spark.Spark.post;

/**
 * Created by gz on 01-Jun-16.
 */
public class SettingsController implements Controller {


    ///Singleton
    private static SettingsController controller;

    private SettingsController() {
    }

    public static SettingsController getInstance() {
        if (controller == null) {
            controller = new SettingsController();
        }

        return controller;
    }
    // end of singleton

    public void defineEndpoints() {
        /**
         * Settings page
         */
        get("/settings", (req, res) -> {
            List<Settings> settings = DB.SETTINGS_DAO.queryForAll();
            Map<String, String> map = new HashMap<>();

            settings.forEach((setting) -> {
                map.put(setting.getName(), setting.getValue());
            });

            Map<String, Object> model = new HashMap<>();
            model.put("settings", map);

            return new ModelAndView(model, "settings");
        }, new JadeTemplateEngine());

        post("/settings", (req, res) -> {
            Map<String, String[]> postParam = req.queryMap().toMap();

            // checking on checkboxes, if they're not in the params, we need to
            // delete them
            if (!postParam.containsKey(Settings.USE_AUTH)) {
                DB.SETTINGS_DAO.deleteById(Settings.USE_AUTH);
            }

            if (!postParam.containsKey(Settings.PUSHBULLET)) {
                DB.SETTINGS_DAO.deleteById(Settings.PUSHBULLET);
            }

            if (!postParam.containsKey(Settings.PUSHALOT)) {
                DB.SETTINGS_DAO.deleteById(Settings.PUSHALOT);
            }

            if (!postParam.containsKey(Settings.PUSHOVER)) {
                DB.SETTINGS_DAO.deleteById(Settings.PUSHOVER);
            }

            postParam.forEach((name, value) -> {

                Settings setting = new Settings();
                setting.setName(name);

                if (name.equalsIgnoreCase(Settings.PASSWORD)) {
                    // do some encryption
                } else {
                    setting.setValue(value[0]);
                }
                try {
                    DB.SETTINGS_DAO.createOrUpdate(setting);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });

            res.redirect("/settings");
            return "";
        });
    }
}
