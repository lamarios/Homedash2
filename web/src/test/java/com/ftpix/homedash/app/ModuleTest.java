package com.ftpix.homedash.app;

import com.ftpix.homedash.app.controllers.ModuleController;
import com.ftpix.homedash.app.controllers.ModuleSettingsController;
import com.ftpix.homedash.app.controllers.PageController;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleSettings;
import com.ftpix.homedash.plugins.docker.DockerPlugin;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Unit test for simple App.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ModuleTest {

    @BeforeClass
    public static void before() {
        //Create a simple module with 2 settings, a Page and the 3 default layouts
        try {
            Module module = new Module();
            module.setPluginClass(DockerPlugin.class.getCanonicalName());
            module.setPage(PageController.INSTANCE.get(1));

            ModuleController.INSTANCE.create(module);

            ModuleSettings setting1 = new ModuleSettings();
            setting1.setName("Setting1");
            setting1.setValue("Yo");
            setting1.setModule(module);
            ModuleSettingsController.INSTANCE.create(setting1);

            ModuleSettings setting2 = new ModuleSettings();
            setting1.setName("Setting2");
            setting1.setValue("Yo");
            setting2.setModule(module);
            ModuleSettingsController.INSTANCE.create(setting2);


        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    @Test
    public void aModuleShouldHave2Settings() {

        try {
            var all = ModuleController.INSTANCE.getAll();


            assertTrue("Has 2 settings", all.get(0).getSettings().size() == 2);

        } catch (Exception e) {
            fail("Exception " + e.getMessage());

        }
    }


}
