/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author admin
 */
public class DaoManager {

    protected Session session;
    protected RequestQueueDao queueDao;
    protected RequestQueueLockDao queueLockDao;
    protected AttachmentDataDaoImpl attachmentDataDao;

    public DaoManager() {
    }

    protected Session getConnection() {
        if (this.session == null) {
            this.session = HibernateUtil.getSessionFactory().openSession();
        }
        return this.session;
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
    
    public AttachmentDataDaoImpl getAttachmentDataDao(){
         if (this.attachmentDataDao == null) {
            this.attachmentDataDao = new AttachmentDataDaoImpl(getConnection());
        }

        return this.attachmentDataDao;
    }
    
    public void manualTransactionStart(){
        getConnection().beginTransaction();
    }
    
    public void manualCommit(){
        getConnection().getTransaction().commit();
    }

    public Object executeAndClose(DaoCommand command) {
        try {
            return command.execute(this);
        } finally {
            getConnection().close();
        }
    }

    public Object transaction(DaoCommand command) {
        Transaction transaction = null;
        try {
            transaction = getConnection().beginTransaction();
            Object returnValue = command.execute(this);
            transaction.commit();
            return returnValue;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e; //or wrap it before rethrowing it
        }
    }

    public Object transactionAndClose(DaoCommand command) {
        return executeAndClose(new DaoCommand() {
            public Object execute(DaoManager manager) {
                return manager.transaction(command);
            }
        });
    }

    public void rollback() {
        getConnection().getTransaction().rollback();
    }
}
