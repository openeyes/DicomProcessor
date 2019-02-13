package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.models.Request;
import com.abehrdigital.dicomprocessor.models.RequestRoutine;
import com.abehrdigital.dicomprocessor.models.RequestRoutineExecution;
import com.abehrdigital.dicomprocessor.utils.DaoFactory;
import com.abehrdigital.dicomprocessor.utils.Status;

import javax.persistence.OptimisticLockException;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.abehrdigital.dicomprocessor.utils.StackTraceUtil.getStackTraceAsString;

public class RequestWorker implements Runnable {
    private static final String ENGINE_NAME = "JavaScript";
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
        try {
            while (true) {
                service.beginTransaction();
                service.clearCache();
                Request lockedRequest = service.getRequestWithLock();

                if (lockedRequest == null) {
                    throw new Exception("Failed to lock the request row");
                }

                RequestRoutine routineForProcessing = service.getNextRoutineToProcess();
                if (routineForProcessing != null) {
                    executeRequestRoutine(routineForProcessing);
                } else {
                    break;
                }
            }
        } catch (Exception exception) {
            Logger.getLogger(RequestWorker.class.getName()).log(Level.SEVERE, exception.toString());
        } finally {
            service.shutDown();
            threadListener.deQueue(requestId, successfulRoutineCount, failedRoutineCount);
        }
    }

    private void executeRequestRoutine(RequestRoutine routineForProcessing) {
        String logMessage = "";
        Status routineStatus = Status.FAILED;
        try {
            String routineBody = service.getRoutineBody(routineForProcessing.getRoutineName());
            ScriptEngine engine = new ScriptEngineManager().getEngineByName(ENGINE_NAME);
            StringWriter engineScriptWriter = new StringWriter();
            redirectEngineOutputToWriter(engine, engineScriptWriter);
            engine.put("controller", service);
            engine.eval(routineBody);
            logMessage += engineScriptWriter;
            routineStatus = Status.COMPLETE;
            routineForProcessing.updateFieldsByStatus(routineStatus);
            service.updateRequestRoutine(routineForProcessing);
            //Request table lock released when transaction is committed
            service.commit();
            successfulRoutineCount++;
        } catch (OptimisticLockException lockException) {
            service.rollback();
            logMessage += lockException.toString();
            routineStatus = Status.RETRY;
        } catch (Exception exception) {
            service.rollback();
            Logger.getLogger(RequestWorker.class.getName()).log(Level.SEVERE,
                    "REQUEST WORKER EXCEPTION WHEN EVALUATING JAVASCRIPT ->  " + getStackTraceAsString(exception));
            logMessage += getStackTraceAsString(exception);
        }

        try {
            service.beginTransaction();
            if (routineStatus == Status.FAILED) {
                routineForProcessing.updateFieldsByStatus(routineStatus);
                service.updateRequestRoutine(routineForProcessing);
                failedRoutineCount++;
            }
            service.saveRequestRoutineExecution(createRequestExecution(routineForProcessing, logMessage));
            service.commit();
        } catch (Exception exception) {
            service.rollback();
            Logger.getLogger(RequestWorker.class.getName(), getStackTraceAsString(exception));
        }
    }

    private void redirectEngineOutputToWriter(ScriptEngine engine, StringWriter engineScriptWriter) {
        ScriptContext context = engine.getContext();
        context.setWriter(engineScriptWriter);
        context.setErrorWriter(engineScriptWriter);
    }

    private RequestRoutineExecution createRequestExecution(RequestRoutine routineForProcessing, String logMessage) {
        return new RequestRoutineExecution(
                logMessage,
                routineForProcessing.getId(),
                new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis()),
                routineForProcessing.getStatus(),
                routineForProcessing.getTryCount()
        );
    }
}
