package com.abehrdigital.dicomprocessor;

public class RequestWorkerDaoManager extends BaseDaoManager {
    private RequestDao requestDao;
    private RequestRoutineDao requestRoutineDao;
    private RequestQueueDao requestQueueDao;

    public RequestDao getRequestDao() {
        if (this.requestDao == null) {
            this.requestDao = new RequestDao(getConnection());
        }
        return this.requestDao;
    }

    public RequestRoutineDao getRequestRoutineDao() {
        if (this.requestRoutineDao == null) {
            this.requestRoutineDao = new RequestRoutineDao(getConnection());
        }
        return this.requestRoutineDao;
    }

    public RequestQueueDao getRequestQueueDao() {
        if (this.requestQueueDao == null) {
            this.requestQueueDao = new RequestQueueDao(getConnection());
        }
        return this.requestQueueDao;
    }

    public void manualTransactionStart() {

        if (!getConnection().getTransaction().isActive())
            getConnection().beginTransaction();

    }

    public void manualCommit() {
        getConnection().getTransaction().commit();
    }


//
//    public DicomParser getDicom(String attachmentMnemonic, String bodySite) throws HibernateException {
//        return new DicomParser(getBlob(attachmentMnemonic, bodySite));
//    }
//
//    public Blob getBlob(String attachmentMnemonic, String bodySite) throws HibernateException {
//        return getData(attachmentMnemonic, bodySite).getBlobData();
//    }
//
//    public String getJson(String attachmentMnemonic, String bodySite) throws HibernateException {
//        AttachmentData data = getData(attachmentMnemonic, bodySite);
//        if (data != null && data.getJson() != null) {
//            return data.getJson();
//        } else {
//            return "{}";
//        }
//
//    }
//
//    public AttachmentData getData(String attachmentMnemonic, String bodySite) throws HibernateException {
//        Criteria criteria = databaseConnection.createCriteria(AttachmentData.class);
//
//        if (attachmentMnemonic != null) {
//            criteria.add(Restrictions.eq("attachmentMnemonic", attachmentMnemonic));
//        }
//
//        if (bodySite != null) {
//            criteria.add(Restrictions.eq("bodySiteSnomedType", bodySite));
//        }
//
//        return (AttachmentData) criteria.uniqueResult();
//    }
//
//    public void putJson(
//            String attachmentMnemonic,
//            String json,
//            String attachmentType,
//            String bodySite,
//            String mimeType) throws SQLException {
//        AttachmentData attachmentData = getData(attachmentMnemonic, bodySite);
//
//        if (attachmentData != null) {
//            attachmentData.setJson(json);
//            databaseConnection.save(attachmentData);
//        } else {
//            attachmentData = new AttachmentData.Builder(
//                    id, mimeType,
//                    attachmentMnemonic, attachmentType,
//                    bodySite)
//                    .jsonData(json)
//                    .build();
//            databaseConnection.save(attachmentData);
//        }
//    }
//
//    // EXECUTE SEQUENCE MULTIPLE BY 10 FROM THE LAST ONE
//    public void addRoutine(String routineName) throws HibernateException {
//        RequestRoutine requestRoutine = getRequestRoutine(routineName);
//        if (requestRoutine != null) {
//            resetRequestRoutine(requestRoutine);
//        } else {
//            if (routineInLibraryExists(routineName)) {
//                requestRoutine = new RequestRoutine.Builder(id ,
//                        routineName , "dicom_queue").build();
//
//
//                databaseConnection.save(requestRoutine);
//            } else {
//                throw new HibernateException("Routine in the library doesn't exist");
//            }
//        }
//    }
//
//    private boolean routineInLibraryExists(String routineName) throws HibernateException {
//        return (databaseConnection.get(RoutineLibrary.class, routineName) != null);
//    }
//
//    private RequestRoutine getRequestRoutine(String routineName) throws HibernateException {
//        Criteria criteria = databaseConnection.createCriteria(RequestRoutine.class);
//        criteria.add(Restrictions.eq("routineName", routineName));
//        criteria.add(Restrictions.eq("requestId", id));
//
//        return (RequestRoutine) criteria.uniqueResult();
//    }
//
//    private void resetRequestRoutine(RequestRoutine requestRoutine) throws HibernateException {
//        requestRoutine.reset();
//        databaseConnection.save(requestRoutine);
//    }
//
//    public void putPdf(
//            String attachmentMnemonic,
//            Blob pdfBlob,
//            String attachmentType,
//            String bodySite,
//            String mimeType
//    ) throws HibernateException {
//        AttachmentData attachmentData = getData(attachmentMnemonic, bodySite);
//        if (attachmentData != null) {
//            attachmentData.setBlobData(pdfBlob);
//            databaseConnection.save(attachmentData);
//        } else {
//            attachmentData = new AttachmentData.Builder(
//                    id, mimeType, attachmentMnemonic,
//                    attachmentType, bodySite)
//                    .blobData(pdfBlob).build();
//            databaseConnection.save(attachmentData);
//        }
//    }
}
