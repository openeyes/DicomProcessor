package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.models.RequestRoutine;
import com.abehrdigital.dicomprocessor.models.RequestRoutineExecution;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestWorker implements Runnable {
    private int requestId;
    private final Map<Integer, Runnable> requestIdToThreadSyncMap;
    private RequestWorkerService service;

    public RequestWorker(int requestId, String requestQueueName, Map<Integer, Runnable> requestIdToThreadSyncMap) {
        this.requestId = requestId;
        this.requestIdToThreadSyncMap = requestIdToThreadSyncMap;
        service = new RequestWorkerService(
                DaoFactory.createScriptEngineDaoManager(),
                requestId,
                requestQueueName
        );
    }

    @Override
    public void run() {
        // INFINITY !!!!LOOP
        service.beginTransaction();
        Request lockedRequest = service.getRequestWithLock();

        if (lockedRequest == null) {
            deQueue(0, 0);
        }

        // CHECK IF NOT EMPTY
        RequestRoutine routineForProcessing = service.getNextRoutineToProcess();

        executeRequestRoutine(routineForProcessing);

        // END LOOP
    }

    private void executeRequestRoutine(RequestRoutine routineForProcessing) {


        String routineBody = service.getRoutineBody(routineForProcessing.getRoutineName());
        String logMessage = "";

        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");

            engine.put("controller", service);
            engine.eval(routineBody);
            service.commit();

            routineForProcessing.successfulExecution();
        } catch (Exception exception) {
            service.rollback();
            Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
         "REQUEST WORKER EXCEPTION WHEN EVALUATING JAVASCRIPT ->  " + exception.toString());
            routineForProcessing.failedExecution();
            System.out.println(exception.toString());
            logMessage = exception.toString();
        }

        try {
            service.beginTransaction();
            service.saveRequestRoutineExecution(createRequestExecution(routineForProcessing, logMessage));
            service.updateRequestRoutine(routineForProcessing);
            service.commit();
        } catch (Exception exception){
            service.rollback();
            Logger.getLogger(DicomEngine.class.getName(), "REQUEST WORKER EXCEPTION HERE ->" +
                    " WHEN SAVING REQUEST EXECUTION AND REQUEST ROUTINE " + exception.toString());
            System.out.println(exception.toString());
        }

        deQueue(1,0);
        service.shutDown();
    }

    private RequestRoutineExecution createRequestExecution(RequestRoutine routineForProcessing, String logMessage) {
        return
                new RequestRoutineExecution(
                        logMessage,
                        routineForProcessing.getId(),
                        new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis()),
                        routineForProcessing.getStatus().getExecutionStatus(),
                        routineForProcessing.getTryCount());
    }

    private void deQueue(int successIncrement, int failIncrement) {
        int currentActiveThreads;
        synchronized (requestIdToThreadSyncMap){
            requestIdToThreadSyncMap.remove(requestId);
            currentActiveThreads = requestIdToThreadSyncMap.size();
        }


        service.updateActiveThreadCount(currentActiveThreads);

        //TO DO NEW MIGRATION FOR SUCCESS AND FAILURE THREAD COUNT
//        requestQueue.incrementThreadSuccessAndFailureCount(successIncrement , failIncrement);
//        requestQueue.setTotalActiveThreadCount(currentActiveThreads);
//
//        workerDaoManager.getRequestQueueDao().save(requestQueue);
    }
}
