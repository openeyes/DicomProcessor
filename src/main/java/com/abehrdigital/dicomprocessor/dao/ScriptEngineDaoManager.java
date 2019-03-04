package com.abehrdigital.dicomprocessor.dao;

public class ScriptEngineDaoManager extends BaseDaoManager {
    private RoutineLibraryDao routineLibraryDao;
    private RequestRoutineExecutionDao requestRoutineExecutionDao;
    private AttachmentDataDao attachmentDataDao;
    private RequestRoutineDao requestRoutineDao;
    private RequestDao requestDao;
    private PatientDao patientDao;
    private GenericMedicalReportDao genericMedicalReportDao;

    public ScriptEngineDaoManager() {
    }

    public RoutineLibraryDao getRoutineLibraryDao() {
        if (this.routineLibraryDao == null) {
            this.routineLibraryDao = new RoutineLibraryDao(getConnection());
        }
        return this.routineLibraryDao;
    }

    public RequestRoutineExecutionDao getRequestRoutineExecutionDao() {
        if (this.requestRoutineExecutionDao == null) {
            this.requestRoutineExecutionDao = new RequestRoutineExecutionDao(getConnection());
        }
        return this.requestRoutineExecutionDao;
    }

    public AttachmentDataDao getAttachmentDataDao() {
        if (this.attachmentDataDao == null) {
            this.attachmentDataDao = new AttachmentDataDao(getConnection());
        }
        return this.attachmentDataDao;
    }

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

    public PatientDao getPatientDao() {
        if (this.patientDao == null) {
            this.patientDao = new PatientDao(getConnection());
        }
        return this.patientDao;
    }

    public GenericMedicalReportDao getGenericMedicalReport(){
        if(this.genericMedicalReportDao == null) {
            this.genericMedicalReportDao = new GenericMedicalReportDao(getConnection());
        }
        return this.genericMedicalReportDao;
    }
}
