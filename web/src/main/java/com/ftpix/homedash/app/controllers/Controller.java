package com.ftpix.homedash.app.controllers;

import java.util.List;

/**
 * Created by gz on 01-Jun-16.
 */
public interface Controller<T, V> {
    void defineEndpoints();

    T get(V id) throws Exception;
    List<T> getAll() throws Exception;
    boolean deleteById(V id) throws Exception;
    boolean delete(T object) throws Exception;
    boolean update(T object) throws Exception;
    V create(T object) throws Exception;

}
