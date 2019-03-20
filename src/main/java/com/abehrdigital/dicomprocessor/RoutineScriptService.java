package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.dao.ScriptEngineDaoManager;
import com.abehrdigital.dicomprocessor.exceptions.EmptyKnownFieldsException;
import com.abehrdigital.dicomprocessor.exceptions.InvalidNumberOfRowsAffectedException;
import com.abehrdigital.dicomprocessor.exceptions.NoSearchedFieldsProvidedException;
import com.abehrdigital.dicomprocessor.exceptions.ValuesNotFoundException;
import com.abehrdigital.dicomprocessor.models.AttachmentData;
import com.abehrdigital.dicomprocessor.models.RequestRoutine;
import com.abehrdigital.dicomprocessor.models.RoutineLibrary;
import com.abehrdigital.dicomprocessor.utils.AttachmentDataThumbnailAdder;
import com.abehrdigital.dicomprocessor.utils.PatientSearchApi;
import com.abehrdigital.dicomprocessor.utils.RoutineScriptAccessor;
import org.hibernate.HibernateException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Blob;

public class RoutineScriptService {
    private static final String EMPTY_JSON_STRING = "{}";
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

    public String getJsonIfNullReturnEmptyJson(String attachmentMnemonic, String bodySite) throws Exception {
        AttachmentData attachmentData = getAttachmentDataByAttachmentMnemonicAndBodySite(attachmentMnemonic, bodySite);

        if (attachmentData != null && attachmentData.getJson() != null) {
            return attachmentData.getJson();
        } else {
            return EMPTY_JSON_STRING;
        }
    }

    public DicomParser getDicomParser(String attachmentMnemonic, String bodySite) throws Exception {
        AttachmentData attachmentData = getAttachmentDataByAttachmentMnemonicAndBodySite(attachmentMnemonic, bodySite);
        if (attachmentData == null) {
            throw new Exception("Attachment data was not found with : Attachment Mnemonic = " + attachmentMnemonic +
                    " body site: " + bodySite + " for Request : " + requestId);
        }

        return new DicomParser(attachmentData.getBlobData());
    }

    public AttachmentData getAttachmentDataByAttachmentMnemonicAndBodySite(String attachmentMnemonic, String bodySite)
            throws Exception {
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
            String mimeType) throws Exception {
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

    public void addRoutine(String routineName) throws Exception {
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
                throw new Exception("Routine name: " + routineName +" doesn't exist in routine_library");
            }
        }
    }

    private boolean routineInLibraryExists(String routineName) throws HibernateException, IOException {
        RoutineLibrary routineLibrary = daoManager.getRoutineLibraryDao().get(routineName);
        return daoManager.getRoutineLibraryDao().get(routineName) != null;
    }

    public void putBlob(
            String attachmentMnemonic,
            Blob pdfBlob,
            String attachmentType,
            String bodySite,
            String mimeType
    ) throws Exception {
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

    public void createAndSetThumbnailsOnAttachmentData(AttachmentData attachmentData) throws Exception {
        AttachmentDataThumbnailAdder.addThumbnails(attachmentData);
        daoManager.getAttachmentDataDao().save(attachmentData);
    }

    public void linkAttachmentDataWithEvent(AttachmentData attachmentData, int eventId, String elementTypeClassName)
            throws InvalidNumberOfRowsAffectedException {
        DataAPI dataAPI = new DataAPI(daoManager.getConnection());
        dataAPI.linkAttachmentDataWithEvent(attachmentData, eventId, elementTypeClassName);
    }

    public String getPatientId(String hospitalNumber) throws Exception {
        String patientId;
        try{
            patientId = PatientSearchApi.searchPatient(hospitalNumber);
        } catch (Exception exception) {
            throw new Exception("Patient was not found with Hospital number: " + hospitalNumber , exception);
        }
        return patientId;
    }

    public AttachmentData getEventDataByMedicalReportStudyInstanceUID(String attachmentMnemonic, String studyInstanceUID) {
        Integer requestId = daoManager.getGenericMedicalReportDao().getRequestIdByStudyInstanceUniqueId(studyInstanceUID);
        if (requestId != -1) {
            return getAttachmentDataByAttachmentMnemonicAndRequestId(attachmentMnemonic, requestId);
        } else {
            return null;
        }
    }

    public void deleteEventAttachmentByAttachmentId(int attachmentId) {
        daoManager.getEventAttachmentItemDao().deleteByAttachmentDataId(attachmentId);
    }

    public String createEvent(String eventData) throws Exception, EmptyKnownFieldsException, ValuesNotFoundException,
            InvalidNumberOfRowsAffectedException, NoSearchedFieldsProvidedException {
        //TODO for light intergration
        DataAPI dataAPI = new DataAPI(daoManager.getConnection());
        return dataAPI.magic("1", eventData, daoManager.getConnection());
    }

    public boolean eventIsDeleted(int eventId){
        return daoManager.getEventDao().getNotDeleted(eventId).isEmpty();
    }
}
