package com.abehrdigital.payloadprocessor;

import com.abehrdigital.payloadprocessor.dao.ScriptEngineDaoManager;
import com.abehrdigital.payloadprocessor.models.Request;
import com.abehrdigital.payloadprocessor.models.RequestRoutine;
import com.abehrdigital.payloadprocessor.models.RequestRoutineExecution;
import com.abehrdigital.payloadprocessor.utils.RoutineScriptAccessor;
import org.hibernate.LockMode;
import java.io.IOException;

public class RequestWorkerService {
    private ScriptEngineDaoManager daoManager;
    private int requestId;
    private String requestQueueName;
    private RoutineScriptService scriptService;
    private RoutineScriptAccessor scriptAccessor;

    public RequestWorkerService(ScriptEngineDaoManager daoManager,
                                int requestId,
                                String requestQueueName,
                                RoutineScriptAccessor scriptAccessor) {
        this.daoManager = daoManager;
        this.requestId = requestId;
        this.requestQueueName = requestQueueName;
        this.scriptService = new RoutineScriptService(daoManager, requestId, requestQueueName);
        this.scriptAccessor = scriptAccessor;
    }

    public String getRoutineBody(String routineName) throws IOException {
        return scriptAccessor.getRoutineScript(routineName);
    }

    public RoutineScriptService getScriptService() {
        return scriptService;
    }

    public void beginTransaction() {
        daoManager.transactionStart();
    }

    public void commit() {
        daoManager.commit();
    }

    public void rollback() {
        daoManager.rollback();
    }


    public void saveRequestRoutineExecution(RequestRoutineExecution requestRoutineExecution) {
        daoManager.getRequestRoutineExecutionDao().save(requestRoutineExecution);
    }

    public void updateRequestRoutine(RequestRoutine requestRoutine) {
        daoManager.getRequestRoutineDao().update(requestRoutine);
    }

    public Request getRequestWithLock() {
        return daoManager.getRequestDao().getWithLock(requestId, LockMode.UPGRADE_NOWAIT);
    }

    public RequestRoutine getNextRoutineToProcess() {
        return daoManager.getRequestRoutineDao().getRequestRoutineWithRequestIdForProcessing(requestId, requestQueueName);
    }

    public void clearCache() {
        daoManager.clearSession();
    }

    public void shutDown() {
        daoManager.shutDown();
    }
}
