package com.abehrdigital.payloadprocessor;

import com.abehrdigital.payloadprocessor.dao.*;
import com.abehrdigital.payloadprocessor.exceptions.EmptyKnownFieldsException;
import com.abehrdigital.payloadprocessor.exceptions.InvalidNumberOfRowsAffectedException;
import com.abehrdigital.payloadprocessor.exceptions.NoSearchedFieldsProvidedException;
import com.abehrdigital.payloadprocessor.exceptions.ValuesNotFoundException;
import com.abehrdigital.payloadprocessor.models.*;
import com.abehrdigital.payloadprocessor.models.Event;
import com.abehrdigital.payloadprocessor.utils.*;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.imageio.ImageIO;
import javax.sql.rowset.serial.SerialBlob;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class RoutineScriptService {
    private static final String EMPTY_JSON_STRING = "{}";
    private ScriptEngineDaoManager
            daoManager;
    private int requestId;
    private String requestQueueName;

    public RoutineScriptService(ScriptEngineDaoManager daoManager,
                                int requestId,
                                String requestQueueName) {
        this.daoManager = daoManager;
        this.requestId = requestId;
        this.requestQueueName = requestQueueName;
    }

    public String getTextIfNullReturnEmptyJson(String attachmentMnemonic, String bodySite) throws Exception {
        AttachmentData attachmentData = getAttachmentDataByAttachmentMnemonicAndBodySite(attachmentMnemonic, bodySite);

        if (attachmentData != null && attachmentData.getTextData() != null) {
            return attachmentData.getTextData();
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

        return new DicomParser(attachmentData.getBlobData(), new Study());
    }

    public String getTextData(String attachmentMnemonic, String bodySite) throws Exception {
        AttachmentData attachmentData = getAttachmentDataByAttachmentMnemonicAndBodySite(attachmentMnemonic, bodySite);
        return attachmentData.getTextData();
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

    public void putText(
            String attachmentMnemonic,
            String json,
            String attachmentType,
            String bodySite,
            String mimeType) throws Exception {
        AttachmentData attachmentData = getAttachmentDataByAttachmentMnemonicAndBodySite(attachmentMnemonic, bodySite);

        if (attachmentData != null) {
            attachmentData.setTextData(json);
            daoManager.getAttachmentDataDao().save(attachmentData);
        } else {
            attachmentData = new AttachmentData.Builder(
                    requestId, mimeType,
                    attachmentMnemonic, attachmentType,
                    bodySite)
                    .textData(json)
                    .build();
            daoManager.getAttachmentDataDao().save(attachmentData);
        }
    }

    public void addRoutine(String routineName) throws Exception {
        addRoutine(routineName, false);
    }

    public void addRoutine(String routineName, Boolean isPriority) throws Exception {
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
                daoManager.getRequestRoutineDao().saveWithNewExecutionSequence(requestRoutine, isPriority);
            } else {
                throw new Exception("Routine name: " + routineName + " doesn't exist in routine_library");
            }
        }
    }

    public void addPriorityRoutine(String routineName) throws Exception {
        addRoutine(routineName, true);
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
        int blobHashCode = DicomBlobUtils.getHashCode(pdfBlob);

        if (attachmentData != null) {
            attachmentData.setBlobData(pdfBlob);
            daoManager.getAttachmentDataDao().save(attachmentData);
            attachmentData.setHashCode(blobHashCode);
        } else {
            attachmentData = new AttachmentData.Builder(
                    requestId, mimeType, attachmentMnemonic,
                    attachmentType, bodySite)
                    .blobData(pdfBlob)
                    .hashCode(blobHashCode)
                    .build();
            daoManager.getAttachmentDataDao().save(attachmentData);
        }
    }

    public void createAndSetThumbnailsOnAttachmentData(AttachmentData attachmentData) throws Exception {
        AttachmentDataThumbnailAdder.addThumbnails(attachmentData);
        daoManager.getAttachmentDataDao().save(attachmentData);
    }

    public void linkAttachmentDataWithEvent(AttachmentData attachmentData, int eventId, String elementTypeClassName)
            throws InvalidNumberOfRowsAffectedException, Exception {
        linkAttachmentDataWithEvent(attachmentData, eventId, elementTypeClassName, "OphGeneric");
    }

    public void linkAttachmentDataWithEvent(AttachmentData attachmentData, int eventId, String elementTypeClassName, String eventClassName)
            throws InvalidNumberOfRowsAffectedException, Exception {
        lockEventRow(eventId);

        if (!isAttachmentAlreadyAttached(eventId, attachmentData)) {
            DataAPI dataAPI = new DataAPI(daoManager.getConnection());
            dataAPI.linkAttachmentDataWithEvent(attachmentData, eventId, elementTypeClassName, eventClassName);
        }

    }

    public void linkAttachmentDataWithEventNewGroup(AttachmentData attachmentData, int eventId, String elementTypeClassName)
            throws InvalidNumberOfRowsAffectedException, Exception {
        linkAttachmentDataWithEventNewGroup(attachmentData, eventId, elementTypeClassName, "OphGeneric", null);
    }

    public void linkAttachmentDataWithEventNewGroup(AttachmentData attachmentData, int eventId, String elementTypeClassName, String eventClassName, String eventViewDocumentSet)
            throws InvalidNumberOfRowsAffectedException, Exception {
        lockEventRow(eventId);

        if (!isAttachmentAlreadyAttached(eventId, attachmentData)) {
            DataAPI dataAPI = new DataAPI(daoManager.getConnection());
            dataAPI.linkAttachmentDataWithEventNewGroup(attachmentData, eventId, elementTypeClassName, eventClassName, eventViewDocumentSet);
        }
    }

    private void lockEventRow(int eventId) throws Exception {
        Event event = daoManager.getEventDao().getWithLock(eventId, LockMode.UPGRADE_NOWAIT);
        if (event == null) {
            throw new Exception("Event ID " + eventId + " : Could not get row lock");
        }
    }

    private boolean isAttachmentAlreadyAttached(int eventId, AttachmentData attachmentData) throws SQLException {
        boolean attachmentIsAlreadyAttached = false;
        List<AttachmentData> attachmentsWithSameHashCode =
                daoManager
                        .getAttachmentDataDao()
                        .getAttachmentsByEventIdAndHashcode(eventId, attachmentData.getHashCode());
        byte[] currentAttachmentDataBlobBytes = DicomBlobUtils.convertBlobToByteArray(attachmentData.getBlobData());
        for (AttachmentData attachmentDataWithSameHashcode :
                attachmentsWithSameHashCode) {
            byte[] attachmentBlobAsBytesWithSameHashcode = DicomBlobUtils
                    .convertBlobToByteArray(
                            attachmentDataWithSameHashcode.getBlobData()
                    );
            if (Arrays.equals(currentAttachmentDataBlobBytes, attachmentBlobAsBytesWithSameHashcode)) {
                attachmentIsAlreadyAttached = true;
            }
        }

        return attachmentIsAlreadyAttached;
    }

    public String getPatientId(String hospitalNumber, String gender, String dateOfBirth, String patientIdentifierType) throws Exception {
        String patientId;
        try {
            patientId = PatientSearchApi.searchPatient(hospitalNumber, gender, dateOfBirth,patientIdentifierType);
        } catch (Exception exception) {
            throw new Exception("Patient was not found with Hospital number: " + hospitalNumber + StackTraceUtil.getStackTraceAsString(exception));
        }
        return patientId;
    }

    public String getPatientId(String hospitalNumber, String patientIdentifierType) throws Exception {
        return getPatientId(hospitalNumber, null, null, patientIdentifierType);
    }

    public AttachmentData getEventDataByDeviceInformationStudyInstanceUID(String attachmentMnemonic, String studyInstanceUID) {
        Integer requestId = daoManager.getGenericDeviceInformationDao().getRequestIdByStudyInstanceUniqueId(studyInstanceUID);
        if (requestId != -1) {
            return getAttachmentDataByAttachmentMnemonicAndRequestId(attachmentMnemonic, requestId);
        } else {
            return null;
        }
    }

    public void deleteEventAttachmentByAttachmentId(int attachmentId) {
        List<EventAttachmentItem> eventAttachmentItems = daoManager.getEventAttachmentItemDao().getByAttachmentDataId(attachmentId);
        for (EventAttachmentItem eventAttachmentItem : eventAttachmentItems) {
            int eventAttachmentGroupSize = daoManager
                    .getEventAttachmentItemDao()
                    .getByEventAttachmentGroupId(eventAttachmentItem.getEventAttachmentGroupId())
                    .size();
            if (eventAttachmentGroupSize > 1) {
                daoManager.getEventAttachmentItemDao().delete(eventAttachmentItem);
            }
        }
    }

    public String createEvent(String eventData) throws Exception, EmptyKnownFieldsException, ValuesNotFoundException,
            InvalidNumberOfRowsAffectedException, NoSearchedFieldsProvidedException {
        //TODO for light intergration
        DataAPI dataAPI = new DataAPI(daoManager.getConnection());
        return dataAPI.magic("1", eventData, daoManager.getConnection());
    }

    public boolean eventIsDeleted(int eventId) {
        return daoManager.getEventDao().getNotDeleted(eventId).isEmpty();
    }

    public boolean patientHasEpisodes(int patientId) {
        return !daoManager.getEpisodeDao().getByPatientId(patientId).isEmpty();
    }

    public List<String> searchFiles(String directory, String regex) throws IOException {
        return DirectoryFileNamesReader.read(directory, regex);
    }

    public Blob getFileAsBlob(String directory, String fileName) throws IOException, SQLException {
        File file = new File(directory + "\\" + fileName);
        FileInputStream fileInputStream = new FileInputStream(file);
        return new SerialBlob(IOUtils.toByteArray(fileInputStream));
    }

    public int getCurrentRequestId() {
        return requestId;
    }

    public boolean moveFile(String directoryFrom, String fileName, String directoryTo) throws IOException {
        return moveFileWithRename(directoryFrom, fileName, directoryTo, fileName);
    }

    public boolean moveFileWithRename(String directoryFrom, String fileName, String directoryTo, String newFileName) throws IOException {
        newFileName = newFileName.replaceAll("/", "-");
        Path movedPath = Files.move(
                Paths.get(directoryFrom + "\\" + fileName),
                Paths.get(directoryTo + "\\" + newFileName.replaceAll("/", "-"))
        );

        return movedPath != null;
    }

    public String readTextFromImage(AttachmentData attachmentData, int x, int y, int width, int height, String regex) throws Exception {
        return readTextFromImage(attachmentData.getBlobData(), x, y, width, height, regex);
    }

    public String readTextFromImage(AttachmentData attachmentData, int x, int y, int width, int height) throws Exception {
        return readTextFromImage(attachmentData.getBlobData(), x, y, width, height, ".*");
    }

    public String readTextFromImage(Blob imageBlob, int x, int y, int width, int height) throws Exception {
        return readTextFromImage(imageBlob, x, y, width, height, ".*");
    }

    public String readTextFromImage(Blob imageBlob, int x, int y, int width, int height, String regex) throws Exception {
        InputStream blobBinaryStream = imageBlob.getBinaryStream();
        BufferedImage image = ImageIO.read(blobBinaryStream);
        return readTextFromImage(image, x, y, width, height, regex);
    }

    public String readTextFromImage(BufferedImage image, int x, int y, int width, int height, String regex) throws Exception {
        Rectangle rectangle = new Rectangle(x, y, width, height);
        ImageTextExtractor imageTextExtractor = new ImageTextExtractor();
        String extractedText = imageTextExtractor.read(image, rectangle);
        if (extractedText.matches(regex)) {
            return extractedText;
        } else {
            throw new Exception("Regex doesn't match for (" + extractedText + " ) Regex (" + regex + ")");
        }
    }

    public String readTextFromPdf(Blob pdfBlob, int x, int y, int width, int height) throws Exception {
        PDDocument pdfDocument = PDFUtils.extractPdfDocumentFromBlob(pdfBlob);
        PDFRenderer pdfRenderer = new PDFRenderer(pdfDocument);
        BufferedImage image = pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);
        return readTextFromImage(image, x, y, width, height, ".*");
    }

    public boolean attachmentsContentAreEqual(AttachmentData attachmentData, int attachmentDataIdToCompare) throws SQLException {
        int firstHashCode = attachmentData.getHashCode();
        AttachmentData attachmentData1 = daoManager.getAttachmentDataDao().get(attachmentDataIdToCompare);
        int secondHashCode = attachmentData1.getHashCode();

        return firstHashCode == secondHashCode;
    }

    public int getBiometryEventId(String studyId) throws Exception {
        BiometryImportedEventsDao biometryImportedEventsDao = daoManager.getBiometryImportedEventsDao();
        int eventId = biometryImportedEventsDao.getEventIdByStudyId(studyId);

        if (eventId != -1) {
            return eventId;
        } else {
            throw new Exception("Biometry cannot be found with that study ID (" + studyId + ")");
        }
    }

    public void updateRequestDetails(String requestData) throws ParseException {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(requestData);
        RequestDetailsDao requestDetailsDao = daoManager.getRequestDetailsDao();
        for (Object keyString : jsonObject.keySet()) {
            String key = String.valueOf(keyString);
            String value = String.valueOf(jsonObject.get(key));
            RequestDetails requestDetails = requestDetailsDao.getByName(key, requestId);
            if (requestDetails != null) {
                requestDetails.setValue(value);
            } else {
                requestDetails = new RequestDetails(requestId, key, value);
            }

            requestDetailsDao.save(requestDetails);

        }
    }

    public boolean isDuplicateAttachmentForEvent(int eventId, int hashCode) throws Exception {

        return daoManager.getAttachmentDataDao().attachmentAlreadyExistsWithThisHashCodeAndEventId(eventId, hashCode);
    }

    public void deleteRequestBlobData() throws Exception {
        AttachmentData requestBlob = getAttachmentDataByAttachmentMnemonicAndBodySite("REQUEST_BLOB", null);
        requestBlob.setBlobData(null);
        daoManager.getAttachmentDataDao().save(requestBlob);
    }

    public void voidAllNewRoutines(String currentRoutine) throws Exception {
        List<RequestRoutine> notCompletedRequestRoutines = daoManager.getRequestRoutineDao().findNotCompletedByRequestId(requestId);

        for (RequestRoutine routine : notCompletedRequestRoutines) {
            if (!routine.getRoutineName().equals(currentRoutine)) {
                routine.setStatus(Status.VOID);
                daoManager.getRequestRoutineDao().save(routine);
            }
        }
    }

    public void synchronizedJavaSubroutine(String name, String synchronizationLockMode) throws Exception {

        if (synchronizationLockMode.equals("runtime_sequential")) {
            while (true) {
                // need to catch time out
                RequestRoutineLock routineLock = daoManager.getRequestRoutineLockDao().getWithLock(name, LockMode.UPGRADE_NOWAIT);

                if (routineLock == null) {
                    Session routineLockSession = HibernateUtil.getSessionFactory().openSession();
                    routineLockSession.beginTransaction();
                    try {
                        RequestRoutineLockDao requestRoutineLockDao = new RequestRoutineLockDao(routineLockSession);
                        routineLock = new RequestRoutineLock(name);
                        requestRoutineLockDao.save(routineLock);
                        routineLockSession.getTransaction().commit();
                    } catch (Exception exception) {
                        // Need to catch unique constraint violation
                        throw exception;
                    }
                } else {
                    break;
                }
            }
        } else {
            throw new Exception("INVALID synchronization lock mode");
        }
    }

    public void deleteAttachmentIfNotNeeded(AttachmentData attachmentData) throws Exception {
        AttachmentDataDao attachmentDataDao = daoManager.getAttachmentDataDao();

        if (attachmentDataDao.isNotAttached(attachmentData.getId())) {
            boolean attachmentCanBeDeleted = false;
            List<AttachmentData> attachmentsWithSameHashCode = attachmentDataDao.getAttachmentsThatAreAttachedWithSameHashcode(attachmentData);
            RequestDetails currentAttachmentsStudyInstanceUid = daoManager.getRequestDetailsDao().getByName("study_instance_uid", requestId);
            for (AttachmentData attachmentDataWithSameHashcode :
                    attachmentsWithSameHashCode) {
                int requestIdOfAttachmentWithSameHashcode = attachmentDataWithSameHashcode.getRequestId();
                RequestDetails duplicateAttachmentsStudyInstanceUid = daoManager.getRequestDetailsDao().getByName("study_instance_uid", requestIdOfAttachmentWithSameHashcode);
                if (currentAttachmentsStudyInstanceUid.getValue().equals(duplicateAttachmentsStudyInstanceUid.getValue())) {
                    attachmentCanBeDeleted = true;
                    break;
                }
            }

            if (attachmentCanBeDeleted) {
                attachmentData.setBlobData(null);
                attachmentData.setHashCode(null);
                attachmentDataDao.save(attachmentData);
            }
        }
    }

    public void updateLastModifiedDate(int eventId) {
        Event event = daoManager.getEventDao().get(eventId);
        event.setLastModifiedDate(new Date());
        daoManager.getEventDao().save(event);
    }
}
