package com.ftpix.homedash.app.controllers;


import com.ftpix.homedash.app.Constants;
import com.ftpix.homedash.models.Settings;
import com.ftpix.homedash.db.DB;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.*;
import spark.template.jade.JadeTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.redirect;

/**
 * Created by gz on 01-Jun-16.
 */
public class SettingsController implements Controller<Settings, String> {
    private Logger logger = LogManager.getLogger();
    private final String AUTH_KEY = "auth";


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

            if (!postParam.containsKey(Settings.USE_REMOTE)) {
                deleteById(Settings.USE_REMOTE);
            }

            postParam.forEach((name, value) -> {
                try {

                    Settings setting = new Settings();
                    setting.setName(name);

                    if (name.equalsIgnoreCase(Settings.PASSWORD)) {
                        if (value[0].trim().length() > 0 && postParam.containsKey(Settings.USERNAME) && postParam.get(Settings.USERNAME)[0].trim().length() > 0) {
                            try {
                                String password = hashPassword(postParam.get(Settings.USERNAME)[0].trim(), value[0].trim());
                                setting.setValue(password);
                                logger.info("Setting to save: [{} => {}]", name, password);
                            } catch (NoSuchAlgorithmException e) {
                                logger.error("Can't save password", e);
                            }
                            //only saving this one if we do something with it.
                            createOrUpdate(setting);
                        }
                    } else {
                        logger.info("Setting to save: [{} => {}]", name, value[0]);

                        setting.setValue(value[0]);
                        createOrUpdate(setting);
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });

            res.redirect("/settings");
            return "";
        });


        /**
         * Login form
         */
        Spark.get("/login", (req, res) -> {
            return new ModelAndView(new HashMap<String, String>(), "login");
        }, new JadeTemplateEngine());

        Spark.post("/login", (req, res) -> {

            String username = req.queryParams("username");
            String password = req.queryParams("password");
            String hash = hashPassword(username, password);

            try {
                if (checkPassword(hash)) {
                    logger.info("Logging in successful, redirecting to main");
                    Session session = req.session(true);
                    session.attribute(AUTH_KEY, hash);
                    res.cookie(AUTH_KEY, hash);

                    res.redirect("/");
                    return null;
                }
            } catch (Exception e) {
                logger.error("Logging in failed due to some error", e);
            }
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "Wrong username/password");
            return new ModelAndView(error, "login");
        }, new JadeTemplateEngine());


        /**
         * Loging out !
         */
        Spark.get("/logout", (req, res) -> {
            res.removeCookie(AUTH_KEY);
            req.session().removeAttribute(AUTH_KEY);

            res.redirect("/");
            return null;
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

    public void createOrUpdate(Settings object) throws SQLException {
        DB.SETTINGS_DAO.createOrUpdate(object);
    }


    /**
     * Generates a hash for password
     *
     * @param username
     * @param password
     * @return
     * @throws NoSuchAlgorithmException
     */
    private String hashPassword(String username, String password) throws NoSuchAlgorithmException {
        String toHash = username + Constants.SALT + password;

        return DigestUtils.sha256Hex(toHash);
    }


    /**
     * Checks user cookies and seee if he is allowed to login
     *
     * @param req
     * @return ok, if he is allowed to see what he's trying to see
     */
    public boolean checkSession(Request req, Response res) {
        try {
            Settings useAuth = get(Settings.USE_AUTH);
            if (useAuth != null && useAuth.getValue().equalsIgnoreCase("1")) {
                logger.info("Auth requested, checking if everything is alright;");

                //checking cookie first
                if (req.cookies().containsKey(AUTH_KEY)) {
                    return checkPassword(req.cookie(AUTH_KEY));
                } else if (req.session().attribute(AUTH_KEY) != null) {
                    return checkPassword((String) req.session().attribute(AUTH_KEY));
                } else {
                    res.removeCookie(AUTH_KEY);
                    req.session().removeAttribute(AUTH_KEY);
                    return false;
                }


            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * H
     *
     * @param password hashed password
     * @return
     * @throws SQLException
     */
    private boolean checkPassword(String password) throws SQLException {
        Settings savedPassword = get(Settings.PASSWORD);
        return savedPassword.getValue().equalsIgnoreCase(password);
    }

}
