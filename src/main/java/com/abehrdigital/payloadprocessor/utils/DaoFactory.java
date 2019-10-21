/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.payloadprocessor.utils;

import com.abehrdigital.payloadprocessor.dao.EngineInitialisationDaoManager;
import com.abehrdigital.payloadprocessor.dao.RequestQueueDaoManager;
import com.abehrdigital.payloadprocessor.dao.ScriptEngineDaoManager;

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
