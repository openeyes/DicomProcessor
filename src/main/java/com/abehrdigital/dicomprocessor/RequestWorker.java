package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.models.RequestQueue;
import com.abehrdigital.dicomprocessor.models.RequestRoutine;
import com.abehrdigital.dicomprocessor.models.RequestRoutineExecution;
import com.abehrdigital.dicomprocessor.models.RoutineLibrary;
import com.abehrdigital.dicomprocessor.utils.Status;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Transaction;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestWorker implements Runnable {
    private int requestId;
    private String requestQueueName;
    private Map<Integer, Runnable> requestIdToThreadSyncMap;
    private RequestWorkerDaoManager workerDaoManager;

    public RequestWorker(int requestId, String requestQueueName, Map<Integer, Runnable> requestIdToThreadSyncMap) {
        this.requestId = requestId;
        this.requestQueueName = requestQueueName;
        this.requestIdToThreadSyncMap = requestIdToThreadSyncMap;
    }

    @Override
    public void run() {
        workerDaoManager = new RequestWorkerDaoManager();

        workerDaoManager.manualTransactionStart();
        Request lockedRequest = workerDaoManager.getRequestDao().getWithLock(requestId, LockMode.UPGRADE_NOWAIT);

        if (lockedRequest == null) {
            deQueue(0, 0);
        }

        RequestRoutine routineForProcessing = workerDaoManager
                .getRequestRoutineDao()
                .getRequestRoutineForProcessing(requestId, requestQueueName);

        executeRequestRoutine(routineForProcessing);

        workerDaoManager.manualCommit();
    }

    private void executeRequestRoutine(RequestRoutine routineForProcessing) {
        ScriptEngineController controller = new ScriptEngineController(
                new ScriptEngineDaoManager(),
                routineForProcessing.getRequestId()
        );

        String routineBody = controller.getRoutineBody(routineForProcessing.getRoutineName());
        String status;
        String logMessage = "";
        Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                "ROUTINE BODY!! " + routineBody);

        try {
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("JavaScript");

            engine.put("controller", controller);

            controller.beginTransaction();

            engine.eval(routineBody);

            controller.commit();

            routineForProcessing.succesfulExecution();
        } catch (Exception exception) {
            controller.rollback();
            Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
         "KAS PER BYBYS " + exception.toString());
          //  routineForProcessing.failedExecution();
            System.out.println(exception.toString());
            logMessage = exception.toString();
        }

        try {
            controller.beginTransaction();
            controller.saveRequestRoutineExecution(createRequestExecution(routineForProcessing, logMessage));
            controller.updateRequestRoutine(routineForProcessing);
            controller.commit();
        } catch (Exception exception){
            controller.rollback();
            Logger.getLogger(DicomEngine.class.getName(), "KAS PER BYBYS " + exception.toString());
            System.out.println(exception.toString());
        }

        deQueue(1,0);
        controller.shutDown();

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

        RequestQueue requestQueue = workerDaoManager.getRequestQueueDao().get(requestQueueName);
        //TO DO NEW MIGRATION FOR SUCCESS AND FAILURE THREAD COUNT
//        requestQueue.incrementThreadSuccessAndFailureCount(successIncrement , failIncrement);
        requestQueue.setTotalActiveThreadCount(currentActiveThreads);

        workerDaoManager.getRequestQueueDao().save(requestQueue);


    }
}
