package com.ftpix.homedash.app.controllers;


import com.ftpix.homedash.models.Settings;
import com.ftpix.homedash.db.DB;
import spark.ModelAndView;
import spark.Spark;
import spark.template.jade.JadeTemplateEngine;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.get;
import static spark.Spark.post;

/**
 * Created by gz on 01-Jun-16.
 */
public class SettingsController implements Controller<Settings, String> {


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
        Spark.get("/settings", (req, res) -> {
            List<Settings> settings = getAll();
            Map<String, String> map = new HashMap<>();

            settings.forEach((setting) -> {
                map.put(setting.getName(), setting.getValue());
            });

            Map<String, Object> model = new HashMap<>();
            model.put("settings", map);

            return new ModelAndView(model, "settings");
        }, new JadeTemplateEngine());

        Spark.post("/settings", (req, res) -> {
            Map<String, String[]> postParam = req.queryMap().toMap();

            // checking on checkboxes, if they're not in the params, we need to
            // delete them
            if (!postParam.containsKey(Settings.USE_AUTH)) {
               deleteById(Settings.USE_AUTH);
            }

            if (!postParam.containsKey(Settings.PUSHBULLET)) {
               deleteById(Settings.PUSHBULLET);
            }

            if (!postParam.containsKey(Settings.PUSHALOT)) {
                deleteById(Settings.PUSHALOT);
            }

            if (!postParam.containsKey(Settings.PUSHOVER)) {
                deleteById(Settings.PUSHOVER);
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
                   createOrUpdate(setting);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });

            res.redirect("/settings");
            return "";
        });
    }


    @Override
    public Settings get(String id) throws SQLException {
        return DB.SETTINGS_DAO.queryForId(id);
    }

    @Override
    public List<Settings> getAll() throws SQLException {
        return DB.SETTINGS_DAO.queryForAll();
    }

    @Override
    public boolean deleteById(String id) throws SQLException {
        return DB.SETTINGS_DAO.deleteById(id) == 1;
    }

    @Override
    public boolean delete(Settings object) throws SQLException {
        return DB.SETTINGS_DAO.delete(object) == 1;
    }

    @Override
    public boolean update(Settings object) throws SQLException {
        return DB.SETTINGS_DAO.update(object) == 1;
    }

    @Override
    public String create(Settings object) throws SQLException {
        DB.SETTINGS_DAO.create(object);
        return object.getName();
    }

    public void createOrUpdate(Settings object) throws SQLException{
        DB.SETTINGS_DAO.createOrUpdate(object);
    }


}
