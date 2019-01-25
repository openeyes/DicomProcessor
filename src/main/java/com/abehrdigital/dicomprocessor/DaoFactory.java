/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor;

/**
 *
 * @author admin
 */
public class DaoFactory {
    public static DaoManager createDaoManager(){
        return new DaoManager();
    }

    public static ScriptEngineDaoManager createScriptEngineDaoManager(){
        return new ScriptEngineDaoManager();
    }

    public static RequestWorkerDaoManager createRequestWorkerDaoManager(){
        return new RequestWorkerDaoManager();
    }
}
