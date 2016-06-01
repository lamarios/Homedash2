package com.ftpix.homedash.app.controllers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import com.ftpix.homedash.app.PluginModuleMaintainer;
import com.google.gson.Gson;
import io.gsonfire.GsonFireBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.Layout;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.Page;
import com.ftpix.homedash.plugins.Plugin;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import de.neuland.jade4j.exceptions.JadeException;
import org.eclipse.jetty.http.HttpStatus;
import spark.ModelAndView;
import spark.template.jade.JadeTemplateEngine;

import static spark.Spark.*;
import static spark.Spark.get;
import static spark.Spark.post;

public class LayoutController implements Controller{
    private Logger logger = LogManager.getLogger();
    final Gson gson = new GsonFireBuilder().enableExposeMethodResult().createGsonBuilder().excludeFieldsWithoutExposeAnnotation().create();


    ///Singleton
    private static LayoutController controller;

    private LayoutController(){}

    public static LayoutController getInstance(){
        if(controller == null){
            controller = new LayoutController();
        }

        return controller;
    }
    // end of singleton

    public void defineEndpoints() {
/*
         * Gets the layout of the modules for the current page
		 */
        get("/modules-layout/:page/:width", (req, res) -> {
            int page = Integer.parseInt(req.params("page"));
            int width = Integer.parseInt(req.params("width"));

            logger.info("/modules-layout/{}/{}", page, width);

            List<ModuleLayout> layouts = generatePageLayout(page, width);
            Map<String, Object> model = new HashMap<>();
            model.put("layouts", layouts);
            model.put("plugins", PluginModuleMaintainer.PLUGIN_INSTANCES);
            return new ModelAndView(model, "module-layout");
        }, new JadeTemplateEngine());


        /*
		 * Get HTML of a single module
		 */
        get("/module-content/:moduleId/:size", (req, res) -> {
            int moduleId = Integer.parseInt(req.params("moduleId"));
            String size = req.params("size");

            logger.info("/module-content/{}/{}", moduleId, size);

            return getModuleContent(moduleId, size);
        });

        /*
		 * Get the layout based on the screen and send back the info
		 */
        get("/layout-info/json/:width", (req, res) -> findClosestLayout(Integer.parseInt(req.params("width"))), gson::toJson);

        /**
         * save a layout grid
         */
        post("/save-module-positions/:layoutId", (req, res) -> savePositions(Integer.parseInt(req.params("layoutId")), req.queryParams("data")), gson::toJson);


        /**
         * Displays layout settings
         */
        get("/layout-settings", (req, res) -> {
            Map<String, Object> model = new HashMap<String, Object>();

            model.put("layouts", DB.LAYOUT_DAO.queryForAll());

            return new ModelAndView(model, "layout-settings");

        }, new JadeTemplateEngine());

        /**
         * Adds a new layout
         */
        post("/layout-settings", (req, res) -> {

            Layout layout = new Layout();
            layout.setName(req.queryParams("name"));
            layout.setMaxGridWidth(1);
            DB.LAYOUT_DAO.create(layout);
            return true;
        });

        /**
         * save layout new size
         */

        get("/layout/:layoutId/set-size/:size", (req, res) -> {
            int layoutId = Integer.parseInt(req.params("layoutId"));
            int size = Integer.parseInt(req.params("size"));

            Layout layout = DB.LAYOUT_DAO.queryForId(layoutId);

            if(layout != null){
                layout.setMaxGridWidth(size);
                DB.LAYOUT_DAO.update(layout);
                return true;
            }else{
                res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
                return false;
            }

        });
    }

    /**
     * Generate a list of module layout based on a page number and window width
     *
     * @param page  page user is looking at
     * @param width width of his view port
     * @return
     * @throws SQLException
     */
    public List<ModuleLayout> generatePageLayout(int page, int width) throws SQLException {
        Layout closestLayout = findClosestLayout(width);
        Page pageObject = DB.PAGE_DAO.queryForId(page);

        return generatePageLayout(pageObject, closestLayout);
    }

    public List<ModuleLayout> generatePageLayout(Page page, Layout layout) throws SQLException {
        logger.info("Generating layour for page [{}] and layout[{}]", page.getId(), layout.getName());
        List<ModuleLayout> layouts = new ArrayList<>();

        List<Module> modules = ModuleController.getInstance().getModulesForPage(page);

        modules.forEach((module) -> {
            try {
                layouts.add(getLayoutForModule(layout, module));
            } catch (Exception e) {
                logger.error("Error while trying to get layout for module", e);
            }
        });

        logger.info("{} modules on page {}", modules.size(), page);

        return layouts;
    }


    /**
     * This will fine the ModuleLayout for a single module on a single layout If
     * there isn't it will be created and defaulted to 0x0
     *
     * @param layout
     * @param module
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public ModuleLayout getLayoutForModule(Layout layout, Module module) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {

        logger.info("Getting Layout for module[{}] on {}", module.getId(), layout.getName());
        QueryBuilder<ModuleLayout, Integer> queryBuilder = DB.MODULE_LAYOUT_DAO.queryBuilder();
        Where<ModuleLayout, Integer> where = queryBuilder.where();
        where.eq("layout_id", layout.getId()).and().eq("module_id", module.getId());

        PreparedQuery<ModuleLayout> preparedQuery = queryBuilder.prepare();

        List<ModuleLayout> moduleLayouts = DB.MODULE_LAYOUT_DAO.query(preparedQuery);
        logger.info("Found {} layouts", moduleLayouts.size());
        ModuleLayout moduleLayout = null;
        if (moduleLayouts.isEmpty()) {
            moduleLayout = new ModuleLayout();
            moduleLayout.setLayout(layout);
            moduleLayout.setModule(module);
            moduleLayout.setX(1);
            moduleLayout.setY(1);

            // Getting the smallest available size for this plugin
            Plugin plugin = (Plugin) Class.forName(module.getPluginClass()).newInstance();
            moduleLayout.setSize(PluginController.getInstance().getSmallestAvailableSize(plugin));
            DB.MODULE_LAYOUT_DAO.create(moduleLayout);
        } else {
            moduleLayout = moduleLayouts.get(0);
        }

        logger.info("Layout found: moduleId:[{}] x:[{}] y:[{}] size:[{}]", module.getId(), moduleLayout.getX(), moduleLayout.getY(), moduleLayout.getSize());
        return moduleLayout;

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

        Module module = DB.MODULE_DAO.queryForId(moduleId);

        Plugin plugin = (Plugin) Class.forName(module.getPluginClass()).newInstance();
        return plugin.getView(size);
    }

    /**
     * Saves the positions of the items on the grid
     *
     * @param layoutId
     * @param queryParams
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public boolean savePositions(int layoutId, String queryParams) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        logger.info("savePositions [{}] [{}]", layoutId, queryParams);

        Layout layout = DB.LAYOUT_DAO.queryForId(layoutId);

        String[] split = queryParams.split("-");
        for (String item : split) {
            logger.info("Parsing:[{}]", item);
            String[] itemSplit = item.split(",");
            if (itemSplit.length == 4) {
                int moduleId = Integer.parseInt(itemSplit[0]);
                int x = Integer.parseInt(itemSplit[1]);
                int y = Integer.parseInt(itemSplit[2]);
                String size = itemSplit[3];

                Module module = DB.MODULE_DAO.queryForId(moduleId);
                ModuleLayout ml = getLayoutForModule(layout, module);
                ml.setX(x);
                ml.setY(y);
                ml.setSize(size);
                logger.info("Layout update: moduleId:[{}] x:[{}] y:[{}] size:[{}]", module.getId(), ml.getX(), ml.getY(), ml.getSize());

                DB.MODULE_LAYOUT_DAO.update(ml);
            } else {
                logger.error("Wrong String format !");
                return false;
            }
        }
        return true;
    }
}
