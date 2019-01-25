/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor;

/**
 * @author admin
 */
public class DaoManager extends BaseDaoManager {

    private RequestQueueDao queueDao;
    private RequestQueueLockDao queueLockDao;
    private AttachmentDataDao attachmentDataDao;
    private RequestRoutineDao requestRoutineDao;

    public DaoManager(){

    }

    public RequestQueueDao getQueueDao() {
        if (this.queueDao == null) {
            this.queueDao = new RequestQueueDao(getConnection());
        }
        return this.queueDao;
    }

    public RequestQueueLockDao getQueueLockDao() {
        if (this.queueLockDao == null) {
            this.queueLockDao = new RequestQueueLockDao(getConnection());
        }

        return this.queueLockDao;
    }

    public AttachmentDataDao getAttachmentDataDao() {
        if (this.attachmentDataDao == null) {
            this.attachmentDataDao = new AttachmentDataDao(getConnection());
        }

        return this.attachmentDataDao;
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
