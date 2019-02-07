/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor.dao;

/**
 * @author admin
 */
public class RequestQueueDaoManager extends BaseDaoManager {

    private RequestQueueDao queueDao;
    private RequestRoutineDao requestRoutineDao;

    public RequestQueueDaoManager() {

    }

    public RequestQueueDao getRequestQueueDao() {
        if (this.queueDao == null) {
            this.queueDao = new RequestQueueDao(getConnection());
        }
        return this.queueDao;
    }

    public RequestRoutineDao getRequestRoutineDao() {
        if (this.requestRoutineDao == null) {
            this.requestRoutineDao = new RequestRoutineDao(getConnection());
        }
        return this.requestRoutineDao;
    }

    public void transactionStart() {
        if (!getConnection().getTransaction().isActive())
            getConnection().beginTransaction();
    }

    public void commit() {
        getConnection().getTransaction().commit();
    }

    public void rollback() {
        getConnection().getTransaction().rollback();
    }
}
