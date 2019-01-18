package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.models.AttachmentData;
import com.abehrdigital.dicomprocessor.models.RequestRoutine;
import com.abehrdigital.dicomprocessor.models.RequestRoutineExecution;
import org.hibernate.HibernateException;

import java.sql.Blob;
import java.sql.SQLException;

public class ScriptEngineController {
    private ScriptEngineDaoManager engineDaoManager;
    private int requestId;

    public ScriptEngineController(ScriptEngineDaoManager engineDaoManager, int requestId) {
        this.engineDaoManager = engineDaoManager;
        this.requestId = requestId;
    }

    public String getRoutineBody(String routineName) {
        return engineDaoManager.getRoutineLibraryDao().get(routineName).getRoutineBody();
    }

    public void beginTransaction() {
        engineDaoManager.manualTransactionStart();
    }

    public void commit() {
        engineDaoManager.manualCommit();
    }

    public void rollback() {
        engineDaoManager.rollback();
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
        return engineDaoManager
                .getAttachmentDataDao()
                .getByAttachmentMnemonicAndBodySite(attachmentMnemonic, bodySite , requestId);
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
            engineDaoManager.getAttachmentDataDao().save(attachmentData);
        } else {
            attachmentData = new AttachmentData.Builder(
                    requestId, mimeType,
                    attachmentMnemonic, attachmentType,
                    bodySite)
                    .jsonData(json)
                    .build();
            engineDaoManager.getAttachmentDataDao().save(attachmentData);
        }
    }

        // EXECUTE SEQUENCE MULTIPLE BY 10 FROM THE LAST ONE
    public void addRoutine(String routineName) throws HibernateException {
        RequestRoutine requestRoutine = engineDaoManager.getRequestRoutineDao().findByRoutineNameAndRequestId(requestId, routineName);
        if (requestRoutine != null) {
            engineDaoManager.getRequestRoutineDao().resetRequestRoutine(requestRoutine);
        } else {
            if (routineInLibraryExists(routineName)) {
                requestRoutine = new RequestRoutine.Builder(requestId ,
                        routineName , "dicom_queue").build();

                engineDaoManager.getRequestRoutineDao().save(requestRoutine);
            } else {
                throw new HibernateException("Routine in the library doesn't exist");
            }
        }
    }

    private boolean routineInLibraryExists(String routineName) throws HibernateException {
        return (engineDaoManager.getRoutineLibraryDao().get(routineName) != null);
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
            engineDaoManager.getAttachmentDataDao().save(attachmentData);
        } else {
            attachmentData = new AttachmentData.Builder(
                    requestId, mimeType, attachmentMnemonic,
                    attachmentType, bodySite)
                    .blobData(pdfBlob).build();
            engineDaoManager.getAttachmentDataDao().save(attachmentData);
        }
    }

    public void saveRequestRoutineExecution(RequestRoutineExecution requestRoutineExecution) {
        engineDaoManager.getRequestRoutineExecutionDao().save(requestRoutineExecution);
    }

    public void updateRequestRoutine(RequestRoutine requestRoutine) {
        engineDaoManager.getRequestRoutineDao().update(requestRoutine);
    }

    public void shutDown() {
        engineDaoManager.shutdown();
    }
}
