/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.models.RequestQueue;
import com.abehrdigital.dicomprocessor.models.RequestQueueLock;
import com.abehrdigital.dicomprocessor.models.RequestRoutine;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author admin
 */
public class RequestQueueExecutor {

    private String requestQueueName;
    private DaoManager queueLockDaoManager;
    private DaoManager queueExecutorDaoManager;
    private final Map<Integer, Runnable> requestIdToThreadSyncMap;

    public RequestQueueExecutor(String requestQueueName,
                                DaoManager queueLockDaoManager,
                                DaoManager queueExecutorDaoManager) {
        this.requestQueueName = requestQueueName;
        this.queueLockDaoManager = queueLockDaoManager;
        this.queueExecutorDaoManager = queueExecutorDaoManager;
        requestIdToThreadSyncMap = new HashMap<>();
    }

    public void execute() throws Exception {
        // RequestQueueLocker.lock();
        lockRequestQueue();

        RequestQueue currentRequestQueue = queueExecutorDaoManager.getQueueDao().get(requestQueueName);;

        List<RequestRoutine> requestRoutinesForExecution = queueExecutorDaoManager
                .getRequestRoutineDao()
                .getRoutinesForQueueProcessing(requestQueueName);

        for (RequestRoutine routineForExecution : requestRoutinesForExecution) {
            Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                    "REQUEST ROUTINA FOR EXECUTION  NAME: " + routineForExecution.getRoutineName() + " ID: " +
                            routineForExecution.getId());

            currentRequestQueue = queueExecutorDaoManager.getQueueDao().get(requestQueueName);
            int currentRequestId = routineForExecution.getRequestId();
            int currentActiveThreads = 0;
            boolean requestIndexInActiveThreadMap = false;


            synchronized (requestIdToThreadSyncMap) {
                currentActiveThreads = requestIdToThreadSyncMap.size();
                requestIndexInActiveThreadMap = requestIdToThreadSyncMap.containsKey(currentRequestId);
            }

            if (currentActiveThreads >= currentRequestQueue.getMaximumActiveThreads()) {
                break;
            }

            if (requestIndexInActiveThreadMap) {
                continue;
            }

            Thread requestWorker = new Thread(
                    new RequestWorker(currentRequestId, requestQueueName, requestIdToThreadSyncMap)
                    , "request_id=" + routineForExecution.getRequestId() + " worker thread"
            );

            synchronized (requestIdToThreadSyncMap) {
                requestIdToThreadSyncMap.put(currentRequestId, requestWorker);
                currentActiveThreads = requestIdToThreadSyncMap.size();
            }

            updateExecuteAndActiveThreadCount(currentActiveThreads);

            requestWorker.start();

            Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                    "Request worker start!!");
        }

        if (requestRoutinesForExecution.size() != 0) {
            TimeUnit.MILLISECONDS.sleep(currentRequestQueue.getBusyYieldMs());
        } else {
            TimeUnit.MILLISECONDS.sleep(currentRequestQueue.getIdleYieldMs());
        }

    }

    private void updateExecuteAndActiveThreadCount(int currentActiveThreads) {
        queueExecutorDaoManager.transactionStart();
        RequestQueue currentRequestQueue = queueExecutorDaoManager
                .getQueueDao()
                .get(requestQueueName);

        currentRequestQueue.incrementExecuteCount();
        currentRequestQueue.setTotalActiveThreadCount(currentActiveThreads);
        queueExecutorDaoManager.getQueueDao().update(currentRequestQueue);
        queueExecutorDaoManager.commit();
    }

    // RETURN VOID
    private void lockRequestQueue() throws Exception {
        RequestQueue requestQueue = queueLockDaoManager.getQueueDao().get(requestQueueName);
        if (requestQueue != null) {
            tryLockingRequestQueueTwentyTimes();
        } else {
            throw new Exception("Request queue doesn't exist");
        }
    }

    private void tryLockingRequestQueueTwentyTimes() {
        for (int tryCount = 0; tryCount < 20; tryCount++) {
            try {
                if (establishQueueLock()) {
                    break;
                }
            } catch (HibernateException exception) {
                sleepFiveSeconds();
            }

        }
    }

    private boolean establishQueueLock() {
        queueLockDaoManager.transactionStart();
        RequestQueueLock queueLock = queueLockDaoManager.getQueueLockDao().
                getWithLock(requestQueueName, LockMode.UPGRADE_NOWAIT);

        if (queueLock == null) {
            createRequestQueueLock();
        } else {
            return true;
        }
        return false;
    }

    private void createRequestQueueLock() {
        RequestQueueLock queueLock;
        queueLock = new RequestQueueLock(requestQueueName);
        queueLockDaoManager.getQueueLockDao().save(queueLock);
        queueLockDaoManager.commit();
    }

    private boolean getRequestQueueLocked() {

        return false;
//        RequestQueueLock queueLock = queueLockDaoManager.getQueueLockDao().
//                getWithLock(requestQueueName, LockMode.UPGRADE_NOWAIT);
//
//        if (queueLock == null) {
//            queueLock = new RequestQueueLock(requestQueueName);
//            queueLockDaoManager.getQueueLockDao().save(queueLock);
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
