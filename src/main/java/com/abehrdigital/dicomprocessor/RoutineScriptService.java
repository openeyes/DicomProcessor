package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.dao.ScriptEngineDaoManager;
import com.abehrdigital.dicomprocessor.models.AttachmentData;
import com.abehrdigital.dicomprocessor.models.RequestRoutine;
import org.hibernate.HibernateException;

import java.sql.Blob;

public class RoutineScriptService {
    private ScriptEngineDaoManager daoManager;
    private int requestId;
    private String requestQueueName;

    public RoutineScriptService(ScriptEngineDaoManager daoManager, int requestId, String requestQueueName) {
        this.daoManager = daoManager;
        this.requestId = requestId;
        this.requestQueueName = requestQueueName;
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
                        requestQueueName)
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

    //TODO REMOVE DATE OF BIRTH AND GENDER FROM THE QUERY
    public int getPatientId(int hospitalNumber, int dateOfBirth, String gender) {
        return daoManager.getPatientDao().getIdByHospitalNumber(hospitalNumber, dateOfBirth, gender);
    }

    public String createEvent(String eventData) {
        //TODO for light intergration
        try {
            return DataAPI.magic("1", eventData);
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception: " + e.getStackTrace();
        }
    }

    public String getEmptyEventTemplate() {
        //TODO for light intergration
        return DataAPI.getEventTemplate();
    }
}
