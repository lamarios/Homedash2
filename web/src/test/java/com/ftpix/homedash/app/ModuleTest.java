package com.ftpix.homedash.app;

import com.ftpix.homedash.app.controllers.*;
import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.Layout;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.plugins.docker.DockerPlugin;
import com.ftpix.homedash.models.ModuleSettings;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.*;


/**
 * Unit test for simple App.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ModuleTest {

    @Test
    public void aModuleShouldHave2Settings() {

        try {
            Module module = ModuleController.getInstance().get(1);

            assertTrue("Has 2 settings", module.getSettings().size() == 2);

        } catch (Exception e) {
            fail("Exception " + e.getMessage());

        }
    }


    @Test
    public void bModuleShouldHaveModuleLayout() {

        try {
            Module module = ModuleController.getInstance().get(1);

            Layout desktopLayout = LayoutController.getInstance().get(1);
            Layout tabletLayout = LayoutController.getInstance().get(2);
            Layout mobileLayout = LayoutController.getInstance().get(3);


            assertNotNull("Has desktop layout", ModuleLayoutController.getInstance().getLayoutForModule(desktopLayout, module));
            assertNotNull("Has tablet layout", ModuleLayoutController.getInstance().getLayoutForModule(tabletLayout, module));
            assertNotNull("Has mobile layout", ModuleLayoutController.getInstance().getLayoutForModule(mobileLayout, module));


        } catch (Exception e) {
            fail("Exception " + e.getMessage());

        }

    }

    @Test
    public void zModuleDeleteShouldDeleteSettingsAndModuleLayout() {
        try {
            Module module = ModuleController.getInstance().get(1);

            Layout desktopLayout = LayoutController.getInstance().get(1);
            Layout tabletLayout = LayoutController.getInstance().get(2);
            Layout mobileLayout = LayoutController.getInstance().get(3);


            //This will create layout if it doesn't exist
            ModuleLayoutController.getInstance().getLayoutForModule(desktopLayout, module);
            ModuleLayoutController.getInstance().getLayoutForModule(tabletLayout, module);
            ModuleLayoutController.getInstance().getLayoutForModule(mobileLayout, module);


            ModuleController.getInstance().delete(module);

            QueryBuilder<ModuleLayout, Integer> queryBuilder = DB.MODULE_LAYOUT_DAO.queryBuilder();
            Where<ModuleLayout, Integer> where = queryBuilder.where();
            where.eq("module_id", module.getId());

            PreparedQuery<ModuleLayout> preparedQuery = queryBuilder.prepare();


            QueryBuilder<ModuleSettings, Integer> settingsQueryBuilder = DB.MODULE_SETTINGS_DAO.queryBuilder();
            Where<ModuleSettings, Integer> where2 = settingsQueryBuilder.where();
            where2.eq("module_id", module.getId());

            PreparedQuery<ModuleSettings> settingsPreapredQuery = settingsQueryBuilder.prepare();

            assertTrue("Module layouts don't exist anymore", DB.MODULE_LAYOUT_DAO.query(preparedQuery).isEmpty());
            assertTrue("Module settings don't exist anymore", DB.MODULE_SETTINGS_DAO.query(settingsPreapredQuery).isEmpty());


        } catch (Exception e) {

        }
    }


    @BeforeClass
    public static void before() {
        //Create a simple module with 2 settings, a Page and the 3 default layouts
        try {
            App.createDefaultData();
            Module module = new Module();
            module.setId(1);
            module.setPluginClass(DockerPlugin.class.getCanonicalName());
            module.setPage(PageController.getInstance().get(1));

            ModuleController.getInstance().create(module);

            ModuleSettings setting1 = new ModuleSettings();
            setting1.setName("Setting1");
            setting1.setValue("Yo");
            setting1.setModule(module);
            ModuleSettingsController.getInstance().create(setting1);

            ModuleSettings setting2 = new ModuleSettings();
            setting1.setName("Setting2");
            setting1.setValue("Yo");
            setting2.setModule(module);
            ModuleSettingsController.getInstance().create(setting2);


        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }


}
