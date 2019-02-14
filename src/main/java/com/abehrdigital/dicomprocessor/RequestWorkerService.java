package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.dao.ScriptEngineDaoManager;
import com.abehrdigital.dicomprocessor.models.*;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;

import java.sql.Blob;

public class RequestWorkerService {
    private ScriptEngineDaoManager daoManager;
    private int requestId;
    private String requestQueueName;

    public RequestWorkerService(ScriptEngineDaoManager daoManager, int requestId, String requestQueueName) {
        this.daoManager = daoManager;
        this.requestId = requestId;
        this.requestQueueName = requestQueueName;
    }

    public void createEvent(String eventData) {
        DataAPI.magic("1", eventData);
    }

    public String getEventTemplate() {
        return DataAPI.getEventTemplate();
    }

    public String getRoutineBody(String routineName) {
        return daoManager.getRoutineLibraryDao().get(routineName).getRoutineBody();
    }

    public void beginTransaction() {
        daoManager.manualTransactionStart();
    }

    public void commit() {
        daoManager.manualCommit();
    }

    public void rollback() {
        daoManager.rollback();
    }

    public String getJson(String attachmentMnemonic, String bodySite) throws HibernateException {
        AttachmentData attachmentData = getAttachmentDataByAttachmentMnemonicAndBodySite(attachmentMnemonic, bodySite);

        if (attachmentData != null && attachmentData.getJson() != null) {
            return attachmentData.getJson();
        } else {
            return "{}";
        }

    }

    public Blob getBlob(String attachmentMnemonic, String bodySite) throws HibernateException {
        return getAttachmentDataByAttachmentMnemonicAndBodySite(attachmentMnemonic, bodySite).getBlobData();
    }

    public DicomParser getDicom(String attachmentMnemonic, String bodySite) throws HibernateException {
        return new DicomParser(getBlob(attachmentMnemonic, bodySite));
    }

    public AttachmentData getAttachmentDataByAttachmentMnemonicAndBodySite(String attachmentMnemonic, String bodySite)
            throws HibernateException {
        return daoManager
                .getAttachmentDataDao()
                .getByAttachmentMnemonicAndBodySite(attachmentMnemonic, bodySite, requestId);
    }

    public void putJson(
            String attachmentMnemonic,
            String json,
            String attachmentType,
            String bodySite,
            String mimeType) {
        AttachmentData attachmentData = getAttachmentDataByAttachmentMnemonicAndBodySite(attachmentMnemonic, bodySite);

        if (attachmentData != null) {
            attachmentData.setJson(json);
            daoManager.getAttachmentDataDao().save(attachmentData);
        } else {
            attachmentData = new AttachmentData.Builder(
                    requestId, mimeType,
                    attachmentMnemonic, attachmentType,
                    bodySite)
                    .jsonData(json)
                    .build();
            daoManager.getAttachmentDataDao().save(attachmentData);
        }
    }

    public void addRoutine(String routineName) throws HibernateException {
        RequestRoutine requestRoutine = daoManager.getRequestRoutineDao().findByRoutineNameAndRequestId(requestId, routineName);
        if (requestRoutine != null) {
            daoManager.getRequestRoutineDao().resetRequestRoutine(requestRoutine);
        } else {
            if (routineInLibraryExists(routineName)) {
                requestRoutine = new RequestRoutine.Builder(
                        requestId,
                        routineName,
                        "dicom_queue")
                        .build();

                daoManager.getRequestRoutineDao().saveWithNewExecutionSequence(requestRoutine);
            } else {
                throw new HibernateException("Routine in the library doesn't exist");
            }
        }
    }

    private boolean routineInLibraryExists(String routineName) throws HibernateException {
        return (daoManager.getRoutineLibraryDao().get(routineName) != null);
    }

    public void putPdf(
            String attachmentMnemonic,
            Blob pdfBlob,
            String attachmentType,
            String bodySite,
            String mimeType
    ) throws HibernateException {
        AttachmentData attachmentData = getAttachmentDataByAttachmentMnemonicAndBodySite(attachmentMnemonic, bodySite);
        if (attachmentData != null) {
            attachmentData.setBlobData(pdfBlob);
            daoManager.getAttachmentDataDao().save(attachmentData);
        } else {
            attachmentData = new AttachmentData.Builder(
                    requestId, mimeType, attachmentMnemonic,
                    attachmentType, bodySite)
                    .blobData(pdfBlob).build();
            daoManager.getAttachmentDataDao().save(attachmentData);
        }
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

    public int getPatientId(String firstName , String lastName , int dateOfBirth , String gender){
        return daoManager.getPatientDao().getIdByNameAndDobAndGender(firstName , lastName , dateOfBirth , gender);
    }

    public RequestQueue getRequestQueue() {
        return daoManager.getRequestQueueDao().get(requestQueueName);
    }

    public void clearCache() {
        daoManager.clearSession();
    }

    public void shutDown() {
        daoManager.shutDown();
    }
}
