package com.ftpix.homedash.app.controllers;

import com.ftpix.homedash.models.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.ModelAndView;
import spark.Spark;
import spark.template.jade.JadeTemplateEngine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ftpix.homedash.db.DB.REMOTE_FAVORITE_DAO;

/**
 * Created by gz on 22-Jun-16.
 */
public class RemoteController implements Controller<RemoteFavorite, Integer> {
    private Logger logger = LogManager.getLogger();

    private static final RemoteController instance = new RemoteController();

    private RemoteController() {
    }

    private Gson gson = new GsonBuilder().create();

    public static RemoteController getInstance() {
        return instance;
    }

    @Override
    public void defineEndpoints() {

        Spark.get("/add-remote", (req, res) -> {
            Map<String, Object> model = new HashMap<String, Object>();

            model.put("favorites", getAll());

            return new ModelAndView(model, "add-remote");
        }, new JadeTemplateEngine());


        Spark.post("/remote/browse-remote", "application/json", (req, res) -> {
            String url = req.queryParams("url");
            String key = req.queryParams("key");

            return browseRemote(url, key);
        }, gson::toJson);


        Spark.post("/remote/add", "application/json", (req, res) -> {

            String id = req.queryParams("id");
            String name = req.queryParams("name");
            String url = req.queryParams("url");
            String key = req.queryParams("key");
            String pluginClass = req.queryParams("pluginClass");


            Page page;
            //find page
            int pageId = 1;
            if (req.session().attribute(ModuleController.SESSION_NEW_MODULE_PAGE) != null) {
                pageId = req.session().attribute(ModuleController.SESSION_NEW_MODULE_PAGE);
            }
            page = PageController.getInstance().get(pageId);


            return addRemoteModule(id, name, url, key, pluginClass, page);
        }, gson::toJson);




    }


    @Override
    public RemoteFavorite get(Integer id) throws Exception {
        return REMOTE_FAVORITE_DAO.queryForId(id);
    }

    @Override
    public List<RemoteFavorite> getAll() throws Exception {
        return REMOTE_FAVORITE_DAO.queryForAll();
    }

    @Override
    public boolean deleteById(Integer id) throws Exception {
        return REMOTE_FAVORITE_DAO.deleteById(id) == 1;
    }

    @Override
    public boolean delete(RemoteFavorite object) throws Exception {
        return REMOTE_FAVORITE_DAO.delete(object) == 1;
    }

    @Override
    public boolean update(RemoteFavorite object) throws Exception {
        return REMOTE_FAVORITE_DAO.update(object) == 1;
    }

    @Override
    public Integer create(RemoteFavorite object) throws Exception {
        return REMOTE_FAVORITE_DAO.create(object);
    }


    /**
     * Query a remote instance for its modules
     */
    private Map<String, Object> browseRemote(String url, String key) throws UnirestException {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        if (!url.endsWith("/")) {
            url += "/";
        }

        url += "api/browse";

        logger.info("Browsing remote: url[{}] key[{}]", url, key);

        HttpResponse<JsonNode> response = Unirest.get(url).header(APIController.HEADER_AUTHORIZATION, key).asJson();

        Map<String, Object> remote = new HashMap<>();


        remote.put("name", response.getBody().getObject().getString("name"));

        List<ExposedModule> modules = new ArrayList<>();

        String moduleString = response.getBody().getObject().getJSONArray("modules").toString();

        modules = gson.fromJson(moduleString, modules.getClass());

        remote.put("modules", modules);
        logger.info("Response: \n {}", response.getBody().toString());

        return remote;

    }


    /**
     * Adds a remote module to local instance
     *
     * @param id
     * @param name
     * @param url
     * @param key
     * @return
     */
    private boolean addRemoteModule(String id, String name, String url, String key, String pluginClass, Page page) throws SQLException {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        if (!url.endsWith("/")) {
            url += "/";
        }

        ModuleSettings idSettings = new ModuleSettings("id", id);
        ModuleSettings nameSetting = new ModuleSettings("name", name);
        ModuleSettings urlSettings = new ModuleSettings("url", url);
        ModuleSettings keySettings = new ModuleSettings("key", key);



        Module module = new Module();
        module.setPage(page);
        module.setPluginClass(pluginClass);
        module.setLocation(ModuleLocation.REMOTE);

        ModuleController.getInstance().create(module);

        idSettings.setModule(module);
        nameSetting.setModule(module);
        urlSettings.setModule(module);
        keySettings.setModule(module);

        ModuleSettingsController.getInstance().create(idSettings);
        ModuleSettingsController.getInstance().create(nameSetting);
        ModuleSettingsController.getInstance().create(urlSettings);
        ModuleSettingsController.getInstance().create(keySettings);

        return true;
    }

}
