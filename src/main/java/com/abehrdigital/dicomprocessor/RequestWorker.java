package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.models.Request;
import com.abehrdigital.dicomprocessor.models.RequestRoutine;
import com.abehrdigital.dicomprocessor.models.RequestRoutineExecution;
import com.abehrdigital.dicomprocessor.utils.DaoFactory;
import com.abehrdigital.dicomprocessor.utils.RandomStringGenerator;
import com.abehrdigital.dicomprocessor.utils.Status;

import javax.persistence.OptimisticLockException;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.abehrdigital.dicomprocessor.utils.StackTraceUtil.getStackTraceAsString;

public class RequestWorker implements Runnable {
    private static final String ENGINE_NAME = "JavaScript";
    private static final int JAVA_CLASS_NAME_IN_ENGINE_LENGTH = 6;

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
                    evaluateEngineExecution(executeRequestRoutine(routineForProcessing), routineForProcessing);
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

    private EngineExecution executeRequestRoutine(RequestRoutine routineForProcessing) {
        String logMessage = "";
        Status routineStatus = Status.FAILED;
        StringWriter engineScriptWriter = new StringWriter();
        String javaClassNameInJavaScriptEngine = RandomStringGenerator.generateWithDefaultChars(JAVA_CLASS_NAME_IN_ENGINE_LENGTH);

        try {
            String routineBody = service.getRoutineBodyWithConvertedJavaMethods(
                    routineForProcessing.getRoutineName(),
                    javaClassNameInJavaScriptEngine
            );
            ScriptEngine engine = new ScriptEngineManager().getEngineByName(ENGINE_NAME);
            redirectEngineOutputToWriter(engine, engineScriptWriter);
            engine.put(javaClassNameInJavaScriptEngine, service.getScriptService());
            engine.eval(routineBody);
            logMessage += engineScriptWriter;
            routineStatus = Status.COMPLETE;
            routineForProcessing.updateFieldsByStatus(routineStatus);
            service.updateRequestRoutine(routineForProcessing);
            //Request table lock released when transaction is committed
            service.commit();
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

        return new EngineExecution(routineStatus, logMessage);
    }

    private void evaluateEngineExecution(EngineExecution engineExecution, RequestRoutine routineForProcessing) {
        try {
            service.beginTransaction();
            if (engineExecution.getStatus() == Status.FAILED) {
                routineForProcessing.updateFieldsByStatus(engineExecution.getStatus());
                service.updateRequestRoutine(routineForProcessing);
                failedRoutineCount++;
            } else if (engineExecution.getStatus() == Status.COMPLETE) {
                successfulRoutineCount++;
            }
            service.saveRequestRoutineExecution(createRequestExecution(routineForProcessing, engineExecution.getLog()));
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
