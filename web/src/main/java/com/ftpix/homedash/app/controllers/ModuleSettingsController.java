package com.ftpix.homedash.app.controllers;

import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.ModuleSettings;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * Created by gz on 04-Jun-16.
 */
public enum ModuleSettingsController implements Controller<ModuleSettings, Integer>{
INSTANCE;




    @Override
    public void defineEndpoints() {

    }

    @Override
    public ModuleSettings get(Integer id) throws SQLException {
        return DB.MODULE_SETTINGS_DAO.queryForId(id);
    }

    @Override
    public List<ModuleSettings> getAll() throws SQLException {
        return DB.MODULE_SETTINGS_DAO.queryForAll();
    }

    @Override
    public boolean deleteById(Integer id) throws SQLException {
        return DB.MODULE_SETTINGS_DAO.deleteById(id) == 1;
    }

    @Override
    public boolean delete(ModuleSettings object) throws SQLException {
        return DB.MODULE_SETTINGS_DAO.delete(object) == 1;
    }

    @Override
    public boolean update(ModuleSettings object) throws SQLException {
        return DB.MODULE_SETTINGS_DAO.update(object) == 1;
    }

    @Override
    public Integer create(ModuleSettings object) throws SQLException {
        DB.MODULE_SETTINGS_DAO.create(object);
        return object.getId();
    }

    public boolean deleteMany(Collection<ModuleSettings> objects) throws SQLException{
        return DB.MODULE_SETTINGS_DAO.delete(objects) == objects.size();
    }
}
