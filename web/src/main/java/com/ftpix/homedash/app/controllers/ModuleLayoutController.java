package com.ftpix.homedash.app.controllers;

import com.ftpix.homedash.app.PluginModuleMaintainer;
import com.ftpix.homedash.models.Layout;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.Page;
import com.ftpix.homedash.plugins.Plugin;
import com.google.gson.Gson;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.sun.media.sound.ModelAbstractChannelMixer;
import io.gsonfire.GsonFireBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.template.jade.JadeTemplateEngine;

import java.sql.SQLException;
import java.util.*;

/**
 * Created by gz on 04-Jun-16.
 */
public class ModuleLayoutController implements Controller<ModuleLayout, Integer>{
    private Logger logger = LogManager.getLogger();
    private final Gson gson = new GsonFireBuilder().enableExposeMethodResult().createGsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    ///Singleton
    private static ModuleLayoutController controller;

    private ModuleLayoutController(){}

    public static ModuleLayoutController getInstance(){
        if(controller == null){
            controller = new ModuleLayoutController();
        }
        return controller;
    }
    // end of singleton


    @Override
    public void defineEndpoints() {
        /*
         * Gets the layout of the modules for the current page
		 */
        Spark.get("/modules-layout/:page/:width", "application/json",  this::layoutForPage, gson::toJson);


        /**
         * save a layout grid
         */
        Spark.post("/save-module-positions/:layoutId", (req, res) -> savePositions(Integer.parseInt(req.params("layoutId")), req.queryParams("data")), gson::toJson);
    }

    /**
     * Gets the layout of the modules for the current page.
     * @param req a Spark request {@link Request}
     * @param res a Spark response {@link Response}
     * @return a map with the HTML and the layout information.
     * @throws SQLException
     */
    private Map<String, Object> layoutForPage(Request req, Response res) throws SQLException {
        int page = Integer.parseInt(req.params("page"));
        int width = Integer.parseInt(req.params("width"));

        logger.info("/modules-layout/{}/{}", page, width);

        List<ModuleLayout> layouts = generatePageLayout(page, width);
        Map<String, Object> model = new HashMap<>();
        model.put("layouts", layouts);
        model.put("plugins", PluginModuleMaintainer.getInstance().PLUGIN_INSTANCES);


        JadeTemplateEngine engine = new JadeTemplateEngine();
        String html = engine.render(new ModelAndView(model, "module-layout"));

        Map<String, Object> toJson = new HashMap<String, Object>();
        toJson.put("html", html);
        toJson.put("layout", LayoutController.getInstance().findClosestLayout(width));

        return toJson;
    }

    @Override
    public ModuleLayout get(Integer id) throws SQLException {
        return DB.MODULE_LAYOUT_DAO.queryForId(id);
    }

    @Override
    public List<ModuleLayout> getAll() throws SQLException {
        return DB.MODULE_LAYOUT_DAO.queryForAll();
    }

    @Override
    public boolean deleteById(Integer id) throws SQLException {
        return DB.MODULE_LAYOUT_DAO.deleteById(id) == 1;
    }

    @Override
    public boolean delete(ModuleLayout object) throws SQLException {
        return DB.MODULE_LAYOUT_DAO.delete(object) == 1;
    }

    @Override
    public boolean update(ModuleLayout object) throws SQLException {
        return DB.MODULE_LAYOUT_DAO.update(object) == 1;
    }

    @Override
    public Integer create(ModuleLayout object) throws SQLException {
        DB.MODULE_LAYOUT_DAO.create(object);
        return object.getId();
    }

    public boolean deleteMany(Collection<ModuleLayout> objects) throws SQLException{
        return DB.MODULE_LAYOUT_DAO.delete(objects) == objects.size();
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
            ModuleLayoutController.getInstance().create(moduleLayout);
        } else {
            moduleLayout = moduleLayouts.get(0);
        }

        logger.info("Layout found: moduleId:[{}] x:[{}] y:[{}] size:[{}]", module.getId(), moduleLayout.getX(), moduleLayout.getY(), moduleLayout.getSize());
        return moduleLayout;

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
    public boolean savePositions(int layoutId, String queryParams) throws Exception {
        logger.info("savePositions [{}] [{}]", layoutId, queryParams);

        Layout layout = LayoutController.getInstance().get(layoutId);

        String[] split = queryParams.split("-");
        for (String item : split) {
            logger.info("Parsing:[{}]", item);
            String[] itemSplit = item.split(",");
            if (itemSplit.length == 4) {
                int moduleId = Integer.parseInt(itemSplit[0]);
                int x = Integer.parseInt(itemSplit[1]);
                int y = Integer.parseInt(itemSplit[2]);
                String size = itemSplit[3];

                Module module = ModuleController.getInstance().get(moduleId);
                ModuleLayout ml = getLayoutForModule(layout, module);
                ml.setX(x);
                ml.setY(y);

                Plugin plugin = PluginModuleMaintainer.getInstance().getPluginForModule(module);

                String[] availableSizes = PluginController.getInstance().getPluginSizes(plugin);

                //Checking if the size we're trying to save really exists (sometimes resizing can fail);
                boolean contains = false;
                for(String s:availableSizes){
                    if(s.equalsIgnoreCase(size)){
                        contains = true;
                        break;
                    }
                }

                //if it's not there, we go back to the first and smallest size
                if(contains){
                    ml.setSize(size);
                }else{
                    ml.setSize(availableSizes[0]);
                }




                logger.info("Layout update: moduleId:[{}] x:[{}] y:[{}] size:[{}]", module.getId(), ml.getX(), ml.getY(), ml.getSize());

                update(ml);
            } else {
                logger.error("Wrong String format !");
                return false;
            }
        }
        return true;
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
        Layout closestLayout = LayoutController.getInstance().findClosestLayout(width);
        Page pageObject = DB.PAGE_DAO.queryForId(page);

        return generatePageLayout(pageObject, closestLayout);
    }

    public List<ModuleLayout> generatePageLayout(Page page, Layout layout) throws SQLException {
        logger.info("Generating layour for page [{}] and layout[{}]", page.getId(), layout.getName());
        List<ModuleLayout> layouts = new ArrayList<>();

        List<Module> modules = ModuleController.getInstance().getModulesForPage(page);

        modules.forEach((module) -> {
            try {
                layouts.add(ModuleLayoutController.getInstance().getLayoutForModule(layout, module));
            } catch (Exception e) {
                logger.error("Error while trying to get layout for module", e);
            }
        });

        logger.info("{} modules on page {}", modules.size(), page);

        return layouts;
    }


    /**
     * Gets all the module layouts for a layout
     * @param layout
     * @return
     * @throws SQLException
     */
    public List<ModuleLayout> getModuleLayoutsForLayout(Layout layout) throws SQLException{
        QueryBuilder<ModuleLayout, Integer> query = DB.MODULE_LAYOUT_DAO.queryBuilder();
        PreparedQuery<ModuleLayout> preparedQuery = query.where().eq("layout_id", layout.getId()).prepare();

        return DB.MODULE_LAYOUT_DAO.query(preparedQuery);

    }
}
