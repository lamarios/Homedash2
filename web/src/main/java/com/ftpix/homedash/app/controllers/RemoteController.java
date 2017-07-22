package com.ftpix.homedash.app.controllers;

import com.ftpix.homedash.models.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.template.jade.JadeTemplateEngine;

import java.sql.SQLException;
import java.util.*;

import static com.ftpix.homedash.db.DB.REMOTE_FAVORITE_DAO;

/**
 * Created by gz on 22-Jun-16.
 */
public enum RemoteController implements Controller<RemoteFavorite, Integer> {
    INSTANCE;

    private Logger logger = LogManager.getLogger();


    private Gson gson = new GsonBuilder().create();

    @Override
    public void defineEndpoints() {

        Spark.get("/add-remote", this::addRemotePage, new JadeTemplateEngine());


        Spark.post("/remote/browse-remote", "application/json", (req, res) -> {
            String url = req.queryParams("url");
            String key = req.queryParams("key");

            return browseRemote(url, key);
        }, gson::toJson);


        Spark.post("/remote/add", "application/json", this::addRemoteModule, gson::toJson);

        Spark.post("/remote/save", "application/json", this::saveRemoteModule, gson::toJson);
        Spark.post("/remote/delete", "application/json", this::deleteRemoteModule, gson::toJson);
        Spark.get("/remote/all", (req, resp) -> this.getAll(), gson::toJson);
    }

    private boolean deleteRemoteModule(Request request, Response response) throws Exception {
        String url = request.queryParams("url");
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        if (!url.endsWith("/")) {
            url += "/";
        }

        get(url).ifPresent(fav -> {
            try {
                deleteById(fav.getId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return true;
    }


    private boolean saveRemoteModule(Request request, Response response) throws Exception {
        String url = request.queryParams("url");
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        if (!url.endsWith("/")) {
            url += "/";
        }


        RemoteFavorite favorite = new RemoteFavorite();
        favorite.setApiKey(request.queryParams("apiKey"));
        favorite.setName(request.queryParams("name"));
        favorite.setUrl(url);

        int id = create(favorite);

        logger.info("Created favourite {} with data: {}", id, favorite);

        return true;
    }

    /**
     * Adds a remote module.
     *
     * @param req a Spark Request {@link Request}
     * @param res a Spark Response {@link Response}
     * @return
     * @throws SQLException
     */
    private boolean addRemoteModule(Request req, Response res) throws SQLException {
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
        page = PageController.INSTANCE.get(pageId);


        return addRemoteModule(id, name, url, key, pluginClass, page);
    }

    /**
     * Gets the template for adding a new remote module
     *
     * @param req a Spark Request {@link Request}
     * @param res a Spark Response {@link Response}
     * @return
     * @throws Exception
     */
    private ModelAndView addRemotePage(Request req, Response res) throws Exception {
        Map<String, Object> model = new HashMap<String, Object>();

        model.put("favorites", getAll());

        return new ModelAndView(model, "add-remote");
    }

    @Override
    public RemoteFavorite get(Integer id) throws Exception {
        return REMOTE_FAVORITE_DAO.queryForId(id);
    }

    public Optional<RemoteFavorite> get(String url) throws Exception {
        try {
            System.out.println(url);
            return Optional.ofNullable(REMOTE_FAVORITE_DAO.queryForEq("url", url)).map(l -> l.get(0));
        } catch (Exception e) {
            return Optional.empty();
        }
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
    private Map<String, Object> browseRemote(String url, String key) throws Exception {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        if (!url.endsWith("/")) {
            url += "/";
        }

        Map<String, Object> remote = new HashMap<>();

        remote.put("favourite", get(url).isPresent());

        url += "api/browse";

        logger.info("Browsing remote: url[{}] key[{}]", url, key);

        HttpResponse<JsonNode> response = Unirest.get(url).header(APIController.HEADER_AUTHORIZATION, key).asJson();


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

        ModuleController.INSTANCE.create(module);

        idSettings.setModule(module);
        nameSetting.setModule(module);
        urlSettings.setModule(module);
        keySettings.setModule(module);

        ModuleSettingsController.INSTANCE.create(idSettings);
        ModuleSettingsController.INSTANCE.create(nameSetting);
        ModuleSettingsController.INSTANCE.create(urlSettings);
        ModuleSettingsController.INSTANCE.create(keySettings);

        return true;
    }

}
