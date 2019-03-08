package com.abehrdigital.dicomprocessor.dao;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

import java.util.List;

public class GenericMedicalReportDao {

    private Session session;

    public GenericMedicalReportDao(Session session) {
        this.session = session;
    }

    public Integer getRequestIdByStudyInstanceUniqueId(String studyInstanceUID){
        NativeQuery query = session.createSQLQuery("" +
                "SELECT mdrp.last_request_id" +
                " FROM et_ophgeneric_medical_report mdrp " +
                "WHERE mdrp.study_instance_uid = :study_instance_uid ")
                .setParameter("study_instance_uid", studyInstanceUID);

        List results = query.list();
        if(results.isEmpty()){
            return -1;
        } else {
            return (Integer) results.get(0);
        }
    }
}