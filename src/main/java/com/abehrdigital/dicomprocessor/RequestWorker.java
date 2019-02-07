package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.models.Request;
import com.abehrdigital.dicomprocessor.models.RequestRoutine;
import com.abehrdigital.dicomprocessor.models.RequestRoutineExecution;
import com.abehrdigital.dicomprocessor.utils.DaoFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestWorker implements Runnable {
    private int requestId;
    private final RequestThreadListener threadListener;
    private RequestWorkerService service;
    private int successfulRoutineCount;
    private int failedRoutineCount;

    public RequestWorker(int requestId, String requestQueueName, RequestThreadListener threadListener) {
        this.requestId = requestId;
        this.threadListener = threadListener;
        service = new RequestWorkerService(
                DaoFactory.createScriptEngineDaoManager(),
                requestId,
                requestQueueName
        );
        successfulRoutineCount = 0;
        failedRoutineCount = 0;
    }

    @Override
    public void run() {

        while (true) {
            service.beginTransaction();
            Request lockedRequest = service.getRequestWithLock();

            if (lockedRequest == null) {
                threadListener.deQueue(requestId, successfulRoutineCount, failedRoutineCount);
                break;
            }

            RequestRoutine routineForProcessing = service.getNextRoutineToProcess();

            if (routineForProcessing != null) {
                executeRequestRoutine(routineForProcessing);
            } else {
                break;
            }
        }

        threadListener.deQueue(requestId, successfulRoutineCount, failedRoutineCount);
        service.shutDown();
    }

    private void executeRequestRoutine(RequestRoutine routineForProcessing) {
        String routineBody = service.getRoutineBody(routineForProcessing.getRoutineName());
        String logMessage = "";

        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            engine.put("controller", service);
            //catch std:out
            engine.eval(routineBody);
            service.commit();
            successfulRoutineCount++;
            routineForProcessing.successfulExecution();
        } catch (Exception exception) {
            service.rollback();
            Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                    "REQUEST WORKER EXCEPTION WHEN EVALUATING JAVASCRIPT ->  " + exception.toString());
            failedRoutineCount++;
            routineForProcessing.failedExecution();

            logMessage = exception.toString();
        }

        try {
            service.beginTransaction();
            service.saveRequestRoutineExecution(createRequestExecution(routineForProcessing, logMessage));
            service.updateRequestRoutine(routineForProcessing);
            //Request lock released
            service.commit();
        } catch (Exception exception) {
            service.rollback();
            Logger.getLogger(DicomEngine.class.getName(), "REQUEST WORKER EXCEPTION HERE ->" +
                    " WHEN SAVING REQUEST EXECUTION AND REQUEST ROUTINE " + exception.toString());
            System.out.println(exception.toString());
        }
        service.clearCache();
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
}
