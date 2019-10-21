/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.payloadprocessor.dao;

/**
 * @author admin
 */
public class RequestQueueDaoManager extends BaseDaoManager {

    private RequestQueueDao requestQueueDao;
    private RequestRoutineDao requestRoutineDao;

    public RequestQueueDaoManager() {
    }

    public RequestQueueDao getRequestQueueDao() {
        if (this.requestQueueDao == null) {
            this.requestQueueDao = new RequestQueueDao(getConnection());
        }
        return this.requestQueueDao;
    }

    public RequestRoutineDao getRequestRoutineDao() {
        if (this.requestRoutineDao == null) {
            this.requestRoutineDao = new RequestRoutineDao(getConnection());
        }
        return this.requestRoutineDao;
    }

    @Override
    public void shutDown() {
        requestRoutineDao = null;
        requestQueueDao = null;
        super.shutDown();
    }
}
