/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.models.RequestQueue;
import com.abehrdigital.dicomprocessor.models.RequestQueueLock;
import com.abehrdigital.dicomprocessor.utils.HibernateUtil;
import org.hibernate.LockMode;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author admin
 */
public class RequestQueueExecutor {

    private String requestQueueName;
    private DaoManager daoManager;

    public RequestQueueExecutor(String requestQueueName, DaoManager daoManager) {
        this.requestQueueName = requestQueueName;
        this.daoManager = daoManager;
    }

    public void execute() throws Exception {

//       checkIntegrityOfDbConnection();
        RequestQueue requestQueue = daoManager.getQueueDao().get(requestQueueName);
        if (requestQueue == null) {
            throw new Exception("Request queue doesn't exist");
        } else {
            if (!establishQueueLock()) {
                lockingFailed();
            } else {

            }
        }
    }

//    private void checkIntegrityOfDbConnection() throws HibernateException {
//        if(mDbSessionRequestQueueLock != null){
//            if(!mDbSessionRequestQueueLock.isConnected()){
//                try {
//                    mDbSessionRequestQueueLock.disconnect();
//                } catch (HibernateException exception){
//                    //ignore errors
//                }
//                mDbSessionRequestQueueLock = null;
//            }
//        }
//        
//        if(mDbSessionRequestQueueLock == null){
//            mDbSessionRequestQueueLock = HibernateUtil.getSessionFactory().openSession();
//        }
//    }
    public boolean establishQueueLock() {
        Boolean queueLocked = false;
        for (int tryCount = 0; tryCount < 20; tryCount++) {

            System.out.println("Queue Name " + requestQueueName
                    + " attempting to get request_queue_lock");
            Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                    "Queue Name " + requestQueueName
                    + " attempting to get request_queue_lock");

            daoManager.manualTransactionStart();
            RequestQueueLock queueLock = daoManager.getQueueLockDao().
                    getWithLock(requestQueueName, LockMode.UPGRADE_NOWAIT);
            if (queueLock == null) {
                queueLock = new RequestQueueLock(requestQueueName);
                daoManager.getQueueLockDao().save(queueLock);
                daoManager.manualCommit();
            } else {
                queueLocked = true;
                break;
            }

            sleepFiveSeconds();
        }
        return queueLocked;

    }

    private boolean getRequestQueueLocked() {

        return false;
//        RequestQueueLock queueLock = daoManager.getQueueLockDao().
//                getWithLock(requestQueueName, LockMode.UPGRADE_NOWAIT);
//
//        if (queueLock == null) {
//            queueLock = new RequestQueueLock(requestQueueName);
//            daoManager.getQueueLockDao().save(queueLock);
//            return true;
//        }
    }

    private void lockingFailed() throws Exception {
        System.err.println("Queue Name " + requestQueueName
                + " failed");
        Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                null,
                "Queue Name " + requestQueueName
                + " failed");
        throw new Exception("lock failed");
    }

    private void sleepFiveSeconds() {
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException ex) {
            Logger.getLogger(RequestQueueExecutor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
