package com.ftpix.homedash.app.controllers;

import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.Page;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by gz on 28-May-16.
 */
public class PageController {

    private Logger logger = LogManager.getLogger();

    /**
     * Deletes a specific page
     * @param id
     * @return
     * @throws SQLException
     */
    public boolean deletePage(int id) throws SQLException {
        logger.info("deletePage({})", id);
        Page page = DB.PAGE_DAO.queryForId(id);
        final ModuleController controller = new ModuleController();

        page.getModules().forEach((module)->{

            try {
               controller.deleteModule(module.getId());
            } catch (SQLException e) {
                e.printStackTrace();
            }

        });


        DB.PAGE_DAO.deleteById(id);

        return true;
    }
}

