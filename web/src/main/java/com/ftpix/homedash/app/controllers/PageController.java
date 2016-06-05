package com.ftpix.homedash.app.controllers;

import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.Page;
import com.google.gson.Gson;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import io.gsonfire.GsonFireBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Spark;

import java.sql.SQLException;
import java.util.List;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;

/**
 * Created by gz on 28-May-16.
 */
public class PageController implements Controller<Page, Integer>{

    private Logger logger = LogManager.getLogger();

    private final Gson gson = new GsonFireBuilder().enableExposeMethodResult().createGsonBuilder().excludeFieldsWithoutExposeAnnotation().create();



    ///Singleton
    private static PageController controller;

    private PageController(){}

    public static PageController getInstance(){
        if(controller == null){
            controller = new PageController();
        }
        return controller;
    }
    // end of singleton


    @Override
    public void defineEndpoints(){


        /**
         * gets all pages as json
         */
        Spark.get("/pages", (req, res) -> {
            return getAll();
        }, gson::toJson);


        /**
         * add a pages and return the whole list of pages as json
         */
        Spark.post("/pages/add", (req, res) -> {
            String pageName = req.queryMap("name").value();
            Page page = new Page();
            page.setName(pageName);

            create(page);
            return getAll();
        }, gson::toJson);


        /**
         * Rename a page
         */
        Spark.post("/pages/edit/:id", (req, res) -> {

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

            return "";

        }, gson::toJson);


        /**
         * gets all pages as json
         */
        Spark.delete("/page/:id", (req, res) -> {
            int id = Integer.parseInt(req.params("id"));

            if(id > 1) {
                //Deleting all the modules on this page
                deletePage(id);
            }
            return getAll();
        }, gson::toJson);
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
        return DB.PAGE_DAO.deleteById(id) == 1;
    }

    @Override
    public boolean delete(Page object) throws SQLException {
        return DB.PAGE_DAO.delete(object) == 1;
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
     * @param id
     * @return
     * @throws SQLException
     */
    public boolean deletePage(int id) throws SQLException {
        logger.info("deletePage({})", id);
        Page page = get(id);


        page.getModules().forEach((module)->{

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

