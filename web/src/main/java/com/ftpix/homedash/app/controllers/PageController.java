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

import java.sql.SQLException;
import java.util.List;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;

/**
 * Created by gz on 28-May-16.
 */
public class PageController implements Controller{

    private Logger logger = LogManager.getLogger();

    final Gson gson = new GsonFireBuilder().enableExposeMethodResult().createGsonBuilder().excludeFieldsWithoutExposeAnnotation().create();



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

    public void defineEndpoints(){


        /**
         * gets all pages as json
         */
        get("/pages", (req, res) -> {
            return DB.PAGE_DAO.queryForAll();
        }, gson::toJson);


        /**
         * add a pages and return the whole list of pages as json
         */
        post("/pages/add", (req, res) -> {
            String pageName = req.queryMap("name").value();
            Page page = new Page();
            page.setName(pageName);

            DB.PAGE_DAO.create(page);
            return DB.PAGE_DAO.queryForAll();
        }, gson::toJson);


        /**
         * Rename a page
         */
        post("/pages/edit/:id", (req, res) -> {

            int id = Integer.parseInt(req.params("id"));

            String pageName = req.queryMap("name").value();
            Page page = DB.PAGE_DAO.queryForId(id);

            if (page != null) {
                page.setName(pageName);

                DB.PAGE_DAO.update(page);
                return DB.PAGE_DAO.queryForAll();
            } else {
                res.status(503);
            }

            return "";

        }, gson::toJson);


        /**
         * gets all pages as json
         */
        delete("/page/:id", (req, res) -> {
            int id = Integer.parseInt(req.params("id"));

            if(id > 1) {
                //Deleting all the modules on this page
                deletePage(id);
            }
            return DB.PAGE_DAO.queryForAll();
        }, gson::toJson);
    }


    /**
     * Deletes a specific page
     * @param id
     * @return
     * @throws SQLException
     */
    public boolean deletePage(int id) throws SQLException {
        logger.info("deletePage({})", id);
        Page page = DB.PAGE_DAO.queryForId(id);


        page.getModules().forEach((module)->{

            try {
               ModuleController.getInstance().deleteModule(module.getId());
            } catch (SQLException e) {
                e.printStackTrace();
            }

        });


        DB.PAGE_DAO.deleteById(id);

        return true;
    }
}

