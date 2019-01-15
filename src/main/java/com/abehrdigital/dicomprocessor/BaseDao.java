/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author admin
 */
public interface BaseDao<T , Id> {
    public T get(Id id);
     
    public void save(T entity);
     
    public void update(T entity);
     
    public void delete(T entity);  
}
