package com.ftpix.homedash.app.controllers;

import java.util.List;

/**
 * Created by gz on 01-Jun-16.
 */
public interface Controller<T, V> {
    public void defineEndpoints();

    public T get(V id) throws Exception;
    public List<T> getAll() throws Exception;
    public boolean deleteById(V id) throws Exception;
    public boolean delete(T object) throws Exception;
    public boolean update(T object) throws Exception;
    public V create(T object) throws Exception;

}
