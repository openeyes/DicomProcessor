package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.dao.ScriptEngineDaoManager;
import com.abehrdigital.dicomprocessor.models.AttachmentData;
import com.abehrdigital.dicomprocessor.models.RequestRoutine;
import com.abehrdigital.dicomprocessor.models.RoutineLibrary;
import com.abehrdigital.dicomprocessor.utils.RoutineScriptAccessor;
import org.hibernate.HibernateException;
import org.hibernate.ReplicationMode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Blob;

public class RoutineScriptService {
    private ScriptEngineDaoManager
            daoManager;
    private int requestId;
    private String requestQueueName;
    private RoutineScriptAccessor scriptAccessor;

    public RoutineScriptService(ScriptEngineDaoManager daoManager,
                                int requestId,
                                String requestQueueName,
                                RoutineScriptAccessor scriptAccessor) {
        this.daoManager = daoManager;
        this.requestId = requestId;
        this.requestQueueName = requestQueueName;
        this.scriptAccessor = scriptAccessor;
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

    public AttachmentData getAttachmentDataByAttachmentMnemonicAndRequestId(String attachmentMnemonic, int requestId)
            throws HibernateException {
        return daoManager
                .getAttachmentDataDao()
                .getByAttachmentMnemonicAndBodySite(attachmentMnemonic, null, requestId);
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

    public void addRoutine(String routineName) throws HibernateException, IOException {
        RequestRoutine requestRoutine = daoManager.getRequestRoutineDao().findByRoutineNameAndRequestId(requestId, routineName);
        if (requestRoutine != null) {
            daoManager.getRequestRoutineDao().resetAndSave(requestRoutine);
        } else {
            if (routineInLibraryExists(routineName)) {
                requestRoutine = new RequestRoutine.Builder(
                        requestId,
                        routineName,
                        requestQueueName)
                        .build();
                daoManager.getRequestRoutineDao().saveWithNewExecutionSequence(requestRoutine);
            } else {
                createRoutineIfExistsInFileSystem(routineName);
            }
        }
    }

    private boolean routineInLibraryExists(String routineName) throws HibernateException, IOException {
        RoutineLibrary routineLibrary = daoManager.getRoutineLibraryDao().get(routineName);
        boolean routineExists = daoManager.getRoutineLibraryDao().get(routineName) != null;
        if (!routineExists) {
            createRoutineIfExistsInFileSystem(routineName);
        }
        return routineExists;
    }

    private synchronized void createRoutineIfExistsInFileSystem(String routineName) throws IOException {
        RoutineLibrary routineLibrary = null;
        if (scriptAccessor.routineExists(routineName)) {
            routineLibrary = new RoutineLibrary(routineName,
                    scriptAccessor.getRoutineScriptHashCode(routineName)
            );
            daoManager.getConnection().merge(routineLibrary);
            daoManager.getRoutineLibraryDao().save(routineLibrary);
        } else {
            throw new FileNotFoundException("Request routine: " + routineName + " is missing");
        }
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

    public void createAndSetThumbnailsOnAttachmentData(AttachmentData attachmentData) {

    }

    public void linkAttachmentDataWithEvent(int attachmentId, int eventId, String elementTypeClassName) {

    }

    //TODO REMOVE DATE OF BIRTH AND GENDER FROM THE QUERY
    public int getPatientId(int hospitalNumber, int dateOfBirth, String gender) {
        return daoManager.getPatientDao().getIdByHospitalNumber(hospitalNumber, dateOfBirth, gender);
    }

    public AttachmentData getEventDataByMedicalReportStudyInstanceUID(String attachmentMnemonic, int studyInstanceUID) {
        try {
            Integer requestId = daoManager.getGenericMedicalReportDao().getRequestIdByStudyInstanceUniqueId(studyInstanceUID);
            return getAttachmentDataByAttachmentMnemonicAndRequestId(attachmentMnemonic, requestId);
        } catch (Exception exception) {
            return null;
        }
    }

    public void deleteEventAttachmentByAttachmentId(int attachmentId) {
        daoManager.getEventAttachmentItemDao().deleteByAttachmentDataId(attachmentId);
    }

    public void createEvent(String eventData) {
        //TODO for light intergration
    }

    public String getEmptyEventTemplate() {
        //TODO for light intergration
        return "";
    }
}
