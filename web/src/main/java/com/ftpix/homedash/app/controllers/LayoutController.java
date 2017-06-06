package com.ftpix.homedash.app.controllers;

import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.Layout;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.plugins.Plugin;
import com.google.gson.Gson;
import de.neuland.jade4j.exceptions.JadeException;
import io.gsonfire.GsonFireBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.template.jade.JadeTemplateEngine;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class LayoutController implements Controller<Layout, Integer> {
    private Logger logger = LogManager.getLogger();
    private final Gson gson = new GsonFireBuilder().enableExposeMethodResult().createGsonBuilder().excludeFieldsWithoutExposeAnnotation().create();


    ///Singleton
    private static LayoutController controller;

    private LayoutController() {
    }

    public static LayoutController getInstance() {
        if (controller == null) {
            controller = new LayoutController();
        }

        return controller;
    }
    // end of singleton

    public void defineEndpoints() {



        /*
         * Get HTML of a single module
		 */
        Spark.get("/module-content/:moduleId/:size", this::getModuleHtml);

        /*
         * Get the layout based on the screen and send back the info
		 */
        Spark.get("/layout-info/:width", "application/json", (req, res) -> findClosestLayout(Integer.parseInt(req.params("width"))), gson::toJson);


        /**
         * Displays layout settings
         */
        Spark.get("/layout-settings", this::getLayoutSettings, new JadeTemplateEngine());

        /**
         * Adds a new layout
         */
        Spark.post("/layout-settings", this::saveLayoutSettings);

        /**
         * save layout new size
         */
        Spark.get("/layout/:layoutId/set-size/:size", this::saveLayoutNewSize);


        /**
         * Update layout's name
         */
        Spark.post("/layout/:id/rename", "application/json", this::saveLayoutNewName, gson::toJson);


        Spark.get("/layout/:id/delete", "application/json", this::deleteLayout, gson::toJson);
    }

    /**
     * Deletes a layout.
     * @param req A Spark request
     * @param res A Spark Response
     * @return
     * @throws SQLException
     */
    private boolean deleteLayout(Request req, Response res) throws SQLException {
        int id = Integer.parseInt(req.params("id"));

        Layout layout = get(id);
        if (layout != null && layout.getId() > 3) {
            delete(layout);

            return true;
        } else {
            return false;
        }
    }

    /**
     * Saves the new name of a layout.
     *
     * @param req A Spark request
     * @param res A Spark Response
     * @return
     * @throws SQLException
     */
    private boolean saveLayoutNewName(Request req, Response res) throws SQLException {
        int id = Integer.parseInt(req.params("id"));
        String newName = req.queryParams("name");

        Layout layout = get(id);
        if (layout != null && layout.getId() > 3 && newName.length() > 0) {

            layout.setName(newName);

            update(layout);

            return true;
        } else {
            return false;
        }

    }

    /**
     * Saves size of a layout.
     *
     * @param req A Spark request
     * @param res A Spark Response
     * @return
     * @throws SQLException
     */
    private boolean saveLayoutNewSize(Request req, Response res) throws SQLException {
        int layoutId = Integer.parseInt(req.params("layoutId"));
        int size = Integer.parseInt(req.params("size"));

        Layout layout = get(layoutId);

        if (layout != null) {
            layout.setMaxGridWidth(size);
            update(layout);
            return true;
        } else {
            res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
            return false;
        }
    }

    /**
     * Save layout settings
     *
     * @param req A Spark request
     * @param res A Spark Response
     * @return
     * @throws SQLException
     */
    private boolean saveLayoutSettings(Request req, Response res) throws SQLException {
        Layout layout = new Layout();
        layout.setName(req.queryParams("name"));
        layout.setMaxGridWidth(1);
        create(layout);
        return true;
    }

    /**
     * Get the different layouts
     *
     * @param req A Spark request
     * @param res A Spark Response
     * @return the compiled template for the layout view
     * @throws SQLException
     */
    private ModelAndView getLayoutSettings(Request req, Response res) throws SQLException {
        Map<String, Object> model = new HashMap<String, Object>();

        model.put("layouts", getAll());

        return new ModelAndView(model, "layout-settings");
    }

    /**
     * @param req A Spark request
     * @param res A Spark Response
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws InstantiationException
     * @throws IOException
     * @throws IllegalAccessException
     */
    private String getModuleHtml(Request req, Response res) throws ClassNotFoundException, SQLException, InstantiationException, IOException, IllegalAccessException {
        int moduleId = Integer.parseInt(req.params("moduleId"));
        String size = req.params("size");

        logger.info("/module-content/{}/{}", moduleId, size);

        return getModuleContent(moduleId, size);
    }

    @Override
    public Layout get(Integer id) throws SQLException {
        return DB.LAYOUT_DAO.queryForId(id);
    }

    @Override
    public List<Layout> getAll() throws SQLException {
        return DB.LAYOUT_DAO.queryForAll();
    }

    @Override
    public boolean deleteById(Integer id) throws SQLException {
        return delete(get(id));
    }

    @Override
    public boolean delete(Layout layout) throws SQLException {
        cleanLayout(layout);
        return DB.LAYOUT_DAO.delete(layout) == 1;
    }

    @Override
    public boolean update(Layout layout) throws SQLException {
        return DB.LAYOUT_DAO.update(layout) == 1;
    }

    @Override
    public Integer create(Layout layout) throws SQLException {
        DB.LAYOUT_DAO.create(layout);
        return layout.getId();
    }


    /**
     * Deletes all the module layouts when deleting a layout
     *
     * @param layout
     * @return
     * @throws SQLException
     */
    private boolean cleanLayout(Layout layout) throws SQLException {
        List<ModuleLayout> moduleLayouts = ModuleLayoutController.getInstance().getModuleLayoutsForLayout(layout);

        return ModuleLayoutController.getInstance().deleteMany(moduleLayouts);

    }


    /**
     * Find the closest layout possible for a window width
     *
     * @param width
     * @return
     * @throws SQLException
     */
    public Layout findClosestLayout(int width) throws SQLException {
        List<Layout> layouts = DB.LAYOUT_DAO.queryForAll();

        // sorting for smallest to biggest.
        Collections.sort(layouts, new Comparator<Layout>() {
            @Override
            public int compare(Layout o1, Layout o2) {
                return Integer.compare(o1.getMaxGridWidth(), o2.getMaxGridWidth());
            }
        });

        Layout selected = null;
        for (Layout layout : layouts) {
            if (layout.getActualSize() <= width) {
                selected = layout;
            } else {
                // we're already bigger, exiting the loop
                break;
            }
        }

        logger.info("Layout size: [{}] window size:[{}]", selected.getActualSize(), width);
        return selected;

    }

    /**
     * Gets the module HTML
     *
     * @param moduleId
     * @param size
     * @return
     * @throws SQLException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws JadeException
     * @throws IOException
     */
    public String getModuleContent(int moduleId, String size) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, JadeException, IOException {

        Module module = ModuleController.getInstance().get(moduleId);

        Plugin plugin = (Plugin) Class.forName(module.getPluginClass()).newInstance();
        return plugin.getView(size);
    }


}
