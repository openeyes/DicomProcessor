/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.models.RequestQueue;
import com.abehrdigital.dicomprocessor.models.RequestQueueLock;
import com.abehrdigital.dicomprocessor.models.RequestRoutine;
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
    private Map<Integer, Runnable> requestIdToThreadSyncMap;

    public RequestQueueExecutor(String requestQueueName,
                                DaoManager queueLockDaoManager,
                                DaoManager queueExecutorDaoManager) {
        this.requestQueueName = requestQueueName;
        this.queueLockDaoManager = queueLockDaoManager;
        this.queueExecutorDaoManager = queueExecutorDaoManager;
        requestIdToThreadSyncMap = new HashMap<>();
    }

    public void execute() throws Exception {
        if (establistOrVerifyQueueLockWithThrow()) {
            RequestQueue currentRequestQueue = queueExecutorDaoManager.getQueueDao().get(requestQueueName);
            List<RequestRoutine> requestRoutinesForExecution = queueExecutorDaoManager.getRequestRoutineDao()
                    .getRequestRoutinesForRequestQueueProcessing(requestQueueName);

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
                        new RequestWorker(currentRequestId, requestQueueName , requestIdToThreadSyncMap)
                        , "request_id=" + routineForExecution.getRequestId() + " worker thread"
                );

                synchronized (requestIdToThreadSyncMap) {
                    requestIdToThreadSyncMap.put(currentRequestId, requestWorker);
                    currentActiveThreads = requestIdToThreadSyncMap.size();
                }
                queueExecutorDaoManager.manualTransactionStart();
                currentRequestQueue = queueExecutorDaoManager.getQueueDao().get(requestQueueName);
                currentRequestQueue.incrementExecuteCount();
                currentRequestQueue.setTotalActiveThreadCount(currentActiveThreads);
                queueExecutorDaoManager.getQueueDao().update(currentRequestQueue);
                queueExecutorDaoManager.manualCommit();

                requestWorker.start();
                Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                        "Request worker start!!");
            }
            if (requestRoutinesForExecution.size() != 0) {
                TimeUnit.MILLISECONDS.sleep(currentRequestQueue.getBusyYieldMs());
            } else {
                TimeUnit.MILLISECONDS.sleep(currentRequestQueue.getIdleYieldMs());
            }

        } else {
            throw new Exception();
        }
    }

    // RETURN VOID
    private boolean establistOrVerifyQueueLockWithThrow() throws Exception {
        RequestQueue requestQueue = queueLockDaoManager.getQueueDao().get(requestQueueName);
        if (requestQueue == null) {
            throw new Exception("Request queue doesn't exist");
        } else {
            if (!establishQueueLock()) {
                lockingFailed();
                return false;
            } else {
                return true;
            }
        }
    }

    private boolean establishQueueLock() {
        Boolean queueLocked = false;
        for (int tryCount = 0; tryCount < 20; tryCount++) {

            System.out.println("Queue Name " + requestQueueName
                    + " attempting to get request_queue_lock");
//            Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
//                    "Queue Name " + requestQueueName
//                            + " attempting to get request_queue_lock");

            queueLockDaoManager.manualTransactionStart();
            RequestQueueLock queueLock = queueLockDaoManager.getQueueLockDao().
                    getWithLock(requestQueueName, LockMode.UPGRADE_NOWAIT);
            if (queueLock == null) {
                queueLock = new RequestQueueLock(requestQueueName);
                queueLockDaoManager.getQueueLockDao().save(queueLock);
                queueLockDaoManager.manualCommit();
            } else {
                queueLocked = true;
                break;
            }

            sleepFiveSeconds();
        }

        Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                "OUT  OF THE LOOP");


        return queueLocked;

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
