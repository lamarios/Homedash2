package com.ftpix.homedash.app.controllers;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.ftpix.homedash.app.Constants;
import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.*;
import com.ftpix.homedash.models.export.Export;
import com.ftpix.homedash.models.export.LayoutExport;
import com.ftpix.homedash.models.export.ModuleExport;
import com.ftpix.homedash.models.export.PageExport;
import com.ftpix.homedash.notifications.Notifications;
import com.ftpix.homedash.notifications.implementations.PushBullet;
import com.ftpix.homedash.notifications.implementations.PushOver;
import com.ftpix.homedash.notifications.implementations.Pushalot;
import com.ftpix.homedash.utils.HomeDashTemplateEngine;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by gz on 01-Jun-16.
 */
public enum SettingsController implements Controller<Settings, String> {
    INSTANCE;

    private final String AUTH_KEY = "auth";
    private Logger logger = LogManager.getLogger();
    private Gson gson = new GsonBuilder().create();

    public void defineEndpoints() {
        /**
         * Settings page
         */
        Spark.get("/settings", this::getSettingsPage, new HomeDashTemplateEngine());

        Spark.post("/settings", this::saveSettings);


        /**
         * Login form
         */
        Spark.get("/login", (req, res) -> new ModelAndView(new HashMap<String, String>(), "login"), new HomeDashTemplateEngine());

        Spark.post("/login", this::login, new HomeDashTemplateEngine());

        Spark.get("/export-config", this::exportConfig);

        Spark.get("/config", this::getConfig, gson::toJson);

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

    private Map<String, Object> getConfig(Request request, Response response) throws Exception {
        Map<String, Object> config = new HashMap<>();

        config.put("auth", Optional.ofNullable(get(Settings.USE_AUTH))
                .map(Settings::getValue)
                .map(s -> s.equalsIgnoreCase("1"))
                .orElse(false));

        return config;
    }

    /**
     * Export Homedash config and plugin setup as a json to be used as a environment variable
     *
     * @param request  the http request
     * @param response the http response
     * @return the configuration as JSON
     * @throws SQLException
     */
    private String exportConfig(Request request, Response response) throws SQLException {

        Export export = new Export();

        DB.PAGE_DAO.queryForAll()
                .stream()
                .map(PageExport::fromModel)
                .forEach(export.boards::add);

        DB.LAYOUT_DAO.queryForAll()
                .stream()
                .map(LayoutExport::fromModel)
                .forEach(export.layouts::add);

        DB.MODULE_DAO.queryForAll()
                .stream()
                .map(ModuleExport::fromModel)
                .forEach(export.modules::add);

        export.settings = DB.SETTINGS_DAO.queryForAll()
                .stream()
                .collect(Collectors.toMap(Settings::getName, Settings::getValue));

        Gson gson = new Gson();


        response.header("Content-type", "application/json");
        return gson.toJson(export);
    }

    /**
     * Imports a JSON as homedash config
     *
     * @param json
     * @return
     */
    public boolean importConfig(String json) throws SQLException {

        logger.info("Importing config:\n{}", json);

        Export export = new Gson().fromJson(json, Export.class);

        //delete all settings
        DB.clearTable(Settings.class);
        export.settings.forEach((n, v) -> {
            Settings setting = new Settings();
            setting.setName(n);
            setting.setValue(v);

            try {
                DB.SETTINGS_DAO.createIfNotExists(setting);
            } catch (SQLException e) {
                logger.error("Error while creating setting", e);
                throw new RuntimeException(e);
            }
        });

        DB.clearTable(Page.class);
        export.boards
                .stream()
                .map(PageExport::toModel)
                .forEach(t -> {
                    try {
                        DB.PAGE_DAO.create(t);
                    } catch (SQLException e) {
                        logger.error("Error while creating board", e);
                        throw new RuntimeException(e);
                    }
                });

        DB.clearTable(Layout.class);
        export.layouts
                .stream()
                .map(LayoutExport::toModel)
                .forEach(l -> {
                    try {
                        DB.LAYOUT_DAO.create(l);
                    } catch (SQLException e) {
                        logger.error("Error while creating layout", e);
                        throw new RuntimeException(e);
                    }
                });

        DB.clearTable(Module.class);
        DB.clearTable(ModuleSettings.class);
        DB.clearTable(ModuleLayout.class);
        export.modules
                .forEach(e -> {

                    try {
                        Module m = ModuleExport.toModel(e);

                        DB.MODULE_DAO.create(m);

                        //settings
                        e.settings.forEach((n, v) -> {
                            ModuleSettings ms = new ModuleSettings();
                            ms.setModule(m);
                            ms.setName(n);
                            ms.setValue(v);

                            try {
                                DB.MODULE_SETTINGS_DAO.create(ms);
                                logger.info("created setting id [{}] [{}] for module [{}]", ms.getId(), ms.getName(), m.getId());
                            } catch (SQLException e1) {
                                logger.error("Error while creating module setting", e1);
                                throw new RuntimeException(e1);
                            }
                        });

                        e.layouts
                                .forEach(l -> {
                                    ModuleLayout ml = new ModuleLayout();
                                    ml.setSize(l.size);
                                    ml.setX(l.x);
                                    ml.setY(l.y);

                                    Layout layout = new Layout();
                                    layout.setId(l.layoutId);

                                    ml.setLayout(layout);
                                    ml.setModule(m);

                                    try {
                                        DB.MODULE_LAYOUT_DAO.create(ml);
                                        logger.info("created module layout id[{}]  for module [{}]", ml.getId(), m.getId());
                                    } catch (SQLException e1) {

                                        logger.error("Error while creating module layout", e1);
                                        throw new RuntimeException(e1);
                                    }
                                });

                    } catch (SQLException ex) {
                        logger.error("Error while creating module", ex);
                        throw new RuntimeException(ex);
                    }
                });


        Constants.STATIC_CONFIG = true;
        return true;
    }

    private String createToken() {
        Algorithm algorithm = Algorithm.HMAC256(Constants.SALT);

        LocalDateTime expiry = LocalDateTime.now().plusYears(1);
        return JWT.create()
                .withIssuer("homedash")
                .withExpiresAt(Date.from(expiry.atZone(ZoneId.systemDefault()).toInstant()))
                .sign(algorithm);
    }

    private boolean verifyToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(Constants.SALT);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("homedash")
                    .build(); //Reusable verifier instance
            DecodedJWT jwt = verifier.verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Processes a login request.
     *
     * @param req a Spark {@link Request}
     * @param res a Spark {@link Response}
     * @return null, will redirect to main page if the login is successful, if not will, return the login compiled template with errors
     * @throws NoSuchAlgorithmException
     */
    private ModelAndView login(Request req, Response res) throws NoSuchAlgorithmException {
        String username = req.queryParams("username");
        String password = req.queryParams("password");
        String hash = hashPassword(username, password);

        try {
            if (checkPassword(hash)) {
                logger.info("Logging in successful, redirecting to main");
//                Session session = req.session(true);
//                session.attribute(AUTH_KEY, hash);
//                res.cookie(AUTH_KEY, hash, 31557600);//one year

                Map<String, String> token = new HashMap<String, String>();
                token.put("token", createToken());


                return new ModelAndView(token, "login-success");
            }
        } catch (Exception e) {
            logger.error("Logging in failed due to some error", e);
        }
        Map<String, String> error = new HashMap<String, String>();
        error.put("error", "Wrong username/password");
        res.status(401);
        return new ModelAndView(error, "login");
    }

    /**
     * Save settings from the POST request.
     *
     * @param req a Spark {@link Request}
     * @param res a Spark {@link Response}
     * @return
     * @throws SQLException
     */
    private String saveSettings(Request req, Response res) throws SQLException {
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

        updateNotificationProviders();

        res.redirect("/settings");
        return "";
    }

    /**
     * Gets the template for the settings page.
     *
     * @param req a Spark {@link Request}
     * @param res a Spark {@link Response}
     * @return
     * @throws SQLException
     */
    private ModelAndView getSettingsPage(Request req, Response res) throws SQLException {
        List<Settings> settings = getAll();
        Map<String, String> map = new HashMap<>();

        settings.forEach((setting) -> {
            map.put(setting.getName(), setting.getValue());
        });

        Map<String, Object> model = new HashMap<>();
        model.put("settings", map);
        model.put("about", UpdateController.INSTANCE.getVersion());


        return new ModelAndView(model, "settings");
    }

    public void createOrUpdate(Settings object) throws SQLException {
        DB.SETTINGS_DAO.createOrUpdate(object);
    }


    /**
     * Generates a hash for password
     */
    private String hashPassword(String username, String password) throws NoSuchAlgorithmException {
        String toHash = username + Constants.SALT + password;

        return DigestUtils.sha256Hex(toHash);
    }


    /**
     * Checks user cookies and seee if he is allowed to login
     *
     * @return ok, if he is allowed to see what he's trying to see
     */
    public boolean checkSession(Request req, Response res) {
        try {
            if (!req.requestMethod().equalsIgnoreCase("OPTIONS")) {
                Settings useAuth = get(Settings.USE_AUTH);
                if (useAuth != null && useAuth.getValue().equalsIgnoreCase("1")) {
                    logger.info("Auth requested, checking if everything is alright;");

                    final String token = req.headers("Authorization").replaceAll("Bearer ", "").trim();


                    //checking cookie first
/*
                if (req.cookies().containsKey(AUTH_KEY)) {
                    return checkPassword(req.cookie(AUTH_KEY));
                } else if (req.session().attribute(AUTH_KEY) != null) {
                    return checkPassword(req.session().attribute(AUTH_KEY));
                } else {
                    res.removeCookie(AUTH_KEY);
                    req.session().removeAttribute(AUTH_KEY);
                    return false;
                }
*/
                    return verifyToken(token);

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
     */
    private boolean checkPassword(String password) throws SQLException {
        Settings savedPassword = get(Settings.PASSWORD);
        return savedPassword.getValue().equalsIgnoreCase(password);
    }


    /**
     * Update the notification providers based on the settings
     */
    public void updateNotificationProviders() throws SQLException {
        Notifications.resetRegisteredProvider();

        Settings pushbullet = get(Settings.PUSHBULLET);
        Settings pushbulletApiKey = get(Settings.PUSHBULLET_API_KEY);
        if (pushbullet != null && pushbulletApiKey != null && pushbullet.getValue().equalsIgnoreCase("1")) {
            PushBullet pb = new PushBullet();
            Map<String, String> settings = new HashMap<String, String>();
            settings.put(PushBullet.API_KEY, pushbulletApiKey.getValue());

            if (pb.setSettings(settings)) {
                Notifications.registerProvider(pb);
            }
        }

        Settings pushalot = get(Settings.PUSHALOT);
        Settings pushalotApiKey = get(Settings.PUSHALOT_API_KEY);
        if (pushalot != null && pushalotApiKey != null && pushalot.getValue().equalsIgnoreCase("1")) {
            Pushalot pb = new Pushalot();
            Map<String, String> settings = new HashMap<String, String>();
            settings.put(Pushalot.API_KEY, pushalotApiKey.getValue());

            if (pb.setSettings(settings)) {
                Notifications.registerProvider(pb);
            }
        }

        Settings pushover = get(Settings.PUSHOVER);
        Settings pushoverApplicationToken = get(Settings.PUSHOVER_APP_TOKEN);
        Settings pushoverUserToken = get(Settings.PUSHOVER_API_KEY);
        if (pushover != null && pushoverApplicationToken != null && pushoverUserToken != null && pushover.getValue().equalsIgnoreCase("1")) {
            PushOver pb = new PushOver();
            Map<String, String> settings = new HashMap<String, String>();
            settings.put(PushOver.APPLICATION_TOKEN, pushoverApplicationToken.getValue());
            settings.put(PushOver.USER_TOKEN, pushoverUserToken.getValue());
            if (pb.setSettings(settings)) {
                Notifications.registerProvider(pb);
            }
        }
    }

}
