package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.models.AttachmentData;
import com.abehrdigital.dicomprocessor.models.RequestQueue;
import com.abehrdigital.dicomprocessor.models.RequestRoutine;
import com.abehrdigital.dicomprocessor.models.RequestRoutineExecution;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;

import java.sql.Blob;
import java.sql.SQLException;

public class RequestWorkerService {
    private ScriptEngineDaoManager daoManager;
    private int requestId;
    private String requestQueueName;

    public RequestWorkerService(ScriptEngineDaoManager daoManager, int requestId, String requestQueueName) {
        this.daoManager = daoManager;
        this.requestId = requestId;
        this.requestQueueName = requestQueueName;
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

    private AttachmentData getAttachmentDataByAttachmentMnemonicAndBodySite(String attachmentMnemonic, String bodySite)
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
            String mimeType) throws SQLException {
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

    // EXECUTE SEQUENCE MULTIPLE BY 10 FROM THE LAST ONE
    public void addRoutine(String routineName) throws HibernateException {
        RequestRoutine requestRoutine = daoManager.getRequestRoutineDao().findByRoutineNameAndRequestId(requestId, routineName);
        if (requestRoutine != null) {
            daoManager.getRequestRoutineDao().resetRequestRoutine(requestRoutine);
        } else {
            if (routineInLibraryExists(routineName)) {
                requestRoutine = new RequestRoutine.Builder(requestId,
                        routineName, "dicom_queue").build();

                daoManager.getRequestRoutineDao().save(requestRoutine);
            } else {
                throw new HibernateException("Routine in the library doesn't exist");
            }
        }
    }

    private boolean routineInLibraryExists(String routineName) throws HibernateException {
        return (daoManager.getRoutineLibraryDao().get(routineName) != null);
    }

//    private RequestRoutine getRequestRoutine(String routineName) throws HibernateException {
//        Criteria criteria = databaseConnection.createCriteria(RequestRoutine.class);
//        criteria.add(Restrictions.eq("routineName", routineName));
//        criteria.add(Restrictions.eq("requestId", id));
//
//        return (RequestRoutine) criteria.uniqueResult();
//    }

//    private void resetRequestRoutine(RequestRoutine requestRoutine) throws HibernateException {
//        requestRoutine.reset();
//        databaseConnection.save(requestRoutine);
//    }

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

    public void shutDown() {
        daoManager.shutdown();
    }

    public Request getRequestWithLock() {
        return daoManager.getRequestDao().getWithLock(requestId, LockMode.UPGRADE_NOWAIT);
    }

    public RequestRoutine getNextRoutineToProcess() {
        return daoManager.getRequestRoutineDao().getRequestRoutineForProcessing(requestId, requestQueueName);
    }

    public RequestQueue getRequestQueue() {
        return daoManager.getRequestQueueDao().get(requestQueueName);
    }

    public void updateActiveThreadCount(int currentActiveThreads) {
        RequestQueue requestQueue = getRequestQueue();
        requestQueue.setTotalActiveThreadCount(currentActiveThreads);
        daoManager.getRequestQueueDao().save(requestQueue);
    }
}
