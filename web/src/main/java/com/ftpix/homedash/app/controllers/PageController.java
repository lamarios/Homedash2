package com.ftpix.homedash.app.controllers;

import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.Page;
import com.google.gson.Gson;
import io.gsonfire.GsonFireBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Created by gz on 28-May-16.
 */
public class PageController implements Controller<Page, Integer> {

    private Logger logger = LogManager.getLogger();

    private final Gson gson = new GsonFireBuilder().enableExposeMethodResult().createGsonBuilder().excludeFieldsWithoutExposeAnnotation().create();


    ///Singleton
    private static PageController controller;

    private PageController() {
    }

    public static PageController getInstance() {
        if (controller == null) {
            controller = new PageController();
        }
        return controller;
    }
    // end of singleton


    @Override
    public void defineEndpoints() {


        /**
         * gets all pages as json
         */
        Spark.get("/pages", (req, res) -> getAll(), gson::toJson);

        /**
         * add a pages and return the whole list of pages as json
         */
        Spark.post("/pages/add", this::addPage, gson::toJson);

        /**
         * Rename a page
         */
        Spark.post("/pages/edit/:id", this::renamePage, gson::toJson);

        /**
         * deleete a page  and return all as json
         */
        Spark.delete("/page/:id", this::deletePage, gson::toJson);
    }

    /**
     * Delete a single page then return all the remaining pages
     * @param req a Spark Request {@link Request}
     * @param res a Spark Response {@link Response}
     * @return
     * @throws SQLException
     */
    private List<Page> deletePage(Request req, Response res) throws SQLException {
        int id = Integer.parseInt(req.params("id"));
        //Deleting all the modules on this page
        if (id > 1) deletePage(id);
        return getAll();
    }

    /**
     * Renames a page
     *
     * @param req a Spark Request {@link Request}
     * @param res a Spark Response {@link Response}
     * @return
     * @throws SQLException
     */
    private List<Page> renamePage(Request req, Response res) throws SQLException {
        int id = Integer.parseInt(req.params("id"));

        String pageName = req.queryMap("name").value();
        Page page = get(id);

        if (page != null) {
            page.setName(pageName);

            update(page);
            return getAll();
        } else {
            res.status(503);
        }
        return Collections.emptyList();
    }

    /**
     * Add a new page.
     *
     * @param req a Spark Request {@link Request}
     * @param res a Spark Response {@link Response}
     * @return
     * @throws SQLException
     */
    private List<Page> addPage(Request req, Response res) throws SQLException {
        String pageName = req.queryMap("name").value();
        Page page = new Page();
        page.setName(pageName);

        create(page);
        return getAll();
    }

    @Override
    public Page get(Integer id) throws SQLException {
        return DB.PAGE_DAO.queryForId(id);
    }

    @Override
    public List<Page> getAll() throws SQLException {
        return DB.PAGE_DAO.queryForAll();
    }

    @Override
    public boolean deleteById(Integer id) throws SQLException {
        return deletePage(id);
    }

    @Override
    public boolean delete(Page object) throws SQLException {
        return deletePage(object.getId());
    }

    @Override
    public boolean update(Page object) throws SQLException {
        return DB.PAGE_DAO.update(object) == 1;
    }

    @Override
    public Integer create(Page object) throws SQLException {
        DB.PAGE_DAO.create(object);
        return object.getId();
    }


    /**
     * Deletes a specific page
     *
     * @param id
     * @return
     * @throws SQLException
     */
    public boolean deletePage(int id) throws SQLException {
        logger.info("deletePage({})", id);
        Page page = get(id);


        page.getModules().forEach((module) -> {

            try {
                ModuleController.getInstance().delete(module);
            } catch (Exception e) {
                e.printStackTrace();
            }

        });


        DB.PAGE_DAO.deleteById(id);

        return true;
    }
}

