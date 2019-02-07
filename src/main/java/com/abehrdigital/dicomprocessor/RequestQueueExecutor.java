/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.dao.RequestQueueDaoManager;
import com.abehrdigital.dicomprocessor.models.RequestQueue;
import com.abehrdigital.dicomprocessor.models.RequestRoutine;
import com.abehrdigital.dicomprocessor.utils.DaoFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author admin
 */
public class RequestQueueExecutor implements RequestThreadListener {

    private String requestQueueName;
    private RequestQueueLocker requestQueueLocker;
    private RequestQueueDaoManager daoManager;
    private final Map<Integer, Runnable> requestIdToThreadSyncMap;
    private RequestQueue currentRequestQueue;
    private int currentActiveThreads;
    private int currentRequestId;
    private final static int LOCK_MAXIMUM_TRY_COUNT = 20;

    public RequestQueueExecutor(String requestQueueName) {
        this.requestQueueName = requestQueueName;
        daoManager = DaoFactory.createRequestQueueExecutorDaoManager();
        requestQueueLocker = new RequestQueueLocker(requestQueueName);
        requestIdToThreadSyncMap = new HashMap<>();
    }

    public void execute() {
        try {
            requestQueueLocker.lockWithMaximumTryCount(LOCK_MAXIMUM_TRY_COUNT);

            List<RequestRoutine> requestRoutinesForExecution = daoManager
                    .getRequestRoutineDao()
                    .getRoutinesForQueueProcessing(requestQueueName);

            for (RequestRoutine routineForExecution : requestRoutinesForExecution) {
                currentRequestQueue = getUpToDateRequestQueue();
                currentRequestId = routineForExecution.getRequestId();
                boolean requestIsInActiveThreadMap;

                synchronized (requestIdToThreadSyncMap) {
                    currentActiveThreads = requestIdToThreadSyncMap.size();
                    requestIsInActiveThreadMap = requestIdToThreadSyncMap.containsKey(currentRequestId);
                }

                if (currentActiveThreads >= currentRequestQueue.getMaximumActiveThreads()) {
                    break;
                }

                if (!requestIsInActiveThreadMap) {
                    saveAndStartRequestWorker(createRequestWorkerWithCurrentRequestId());
                    setLastThreadSpawnDateAndRequestId();
                    updateActiveThreadCount();
                    updateCurrentRequestQueue();
                }
            }

            currentRequestQueue = getUpToDateRequestQueue();
            sleepAfterRoutineLoop(requestRoutinesForExecution.size());
        } catch (Exception exception) {
            Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                    exception.toString() + " Executor exception");
        }
    }

    private void setLastThreadSpawnDateAndRequestId() {
        currentRequestQueue.setLastThreadSpawnRequestId(currentRequestId);
        currentRequestQueue.setLastThreadSpawnDateToCurrentTimestamp();
    }

    private void sleepAfterRoutineLoop(int requestRoutineListSize) throws InterruptedException {
        if (requestRoutineListSize != 0) {
            TimeUnit.MILLISECONDS.sleep(currentRequestQueue.getBusyYieldMs());
        } else {
            TimeUnit.MILLISECONDS.sleep(currentRequestQueue.getIdleYieldMs());
        }
    }

    private RequestQueue getUpToDateRequestQueue() {
        return daoManager.getRequestQueueDao().getUpdated(requestQueueName);
    }

    private Thread createRequestWorkerWithCurrentRequestId(){
        return new Thread(
                new RequestWorker(currentRequestId, requestQueueName, this),
                "request_id=" + currentRequestId + " worker thread"
        );
    }

    private void saveAndStartRequestWorker(Thread requestWorker) {
        synchronized (requestIdToThreadSyncMap) {
            requestIdToThreadSyncMap.put(currentRequestId, requestWorker);
            currentActiveThreads = requestIdToThreadSyncMap.size();
        }

        requestWorker.start();
    }

    private void updateActiveThreadCount() {
        currentRequestQueue.setTotalActiveThreadCount(currentActiveThreads);
    }

    @Override
    public void deQueue(int requestId, int successfulRoutineCount, int failedRoutineCount) {
        synchronized (requestIdToThreadSyncMap) {
            requestIdToThreadSyncMap.remove(requestId);
            currentActiveThreads = requestIdToThreadSyncMap.size();
        }

        setActiveThreadAndExecutionCounts(successfulRoutineCount, failedRoutineCount);
        updateCurrentRequestQueue();
    }

    private void setActiveThreadAndExecutionCounts(int successfulRoutineCount, int failedRoutineCount) {
        currentRequestQueue = getUpToDateRequestQueue();
        currentRequestQueue.setTotalActiveThreadCount(currentActiveThreads);
        currentRequestQueue.incrementSuccessCount(successfulRoutineCount);
        currentRequestQueue.incrementFailCount(failedRoutineCount);
        currentRequestQueue.updateTotalExecuteCount();
    }

    private void updateCurrentRequestQueue() {
        daoManager.transactionStart();
        daoManager.getRequestQueueDao().update(currentRequestQueue);
        daoManager.commit();
    }

    public void shutDown() {
        requestQueueLocker.unlock();
        daoManager.shutDown();
    }
}
