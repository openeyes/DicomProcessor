/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor.utils;

import com.abehrdigital.dicomprocessor.dao.EngineInitialisationDaoManager;
import com.abehrdigital.dicomprocessor.dao.RequestQueueDaoManager;
import com.abehrdigital.dicomprocessor.dao.ScriptEngineDaoManager;

/**
 * @author admin
 */
public class DaoFactory {
    public static RequestQueueDaoManager createRequestQueueExecutorDaoManager() {
        return new RequestQueueDaoManager();
    }

    public static ScriptEngineDaoManager createScriptEngineDaoManager() {
        return new ScriptEngineDaoManager();
    }

    public static EngineInitialisationDaoManager createEngineInitialisationDaoManager(){
        return new EngineInitialisationDaoManager();
    }
}
