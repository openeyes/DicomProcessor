package com.abehrdigital.payloadprocessor.dao;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

import java.util.List;

public class GenericDeviceInformationDao {

    private Session session;

    public GenericDeviceInformationDao(Session session) {
        this.session = session;
    }

    public Integer getRequestIdByStudyInstanceUniqueId(String studyInstanceUID){
        NativeQuery query = session.createSQLQuery("" +
                "SELECT mdrp.last_request_id" +
                " FROM et_ophgeneric_device_information mdrp " +
                "WHERE mdrp.study_instance_uid = :study_instance_uid ")
                .setParameter("study_instance_uid", studyInstanceUID);

        List results = query.list();
        if(results.isEmpty()){
            return -1;
        } else {
            return (Integer) results.get(0);
        }
    }

    public Integer getRequestIdByStudyInstanceUniqueIdAndManufacturerModelName(String studyInstanceUID, String manufacturerModelName){
        NativeQuery query = session.createSQLQuery("" +
                "SELECT mdrp.last_request_id" +
                " FROM et_ophgeneric_device_information mdrp " +
                "WHERE mdrp.study_instance_uid = :study_instance_uid AND " +
                "manufacturer_model_name = :manufacturer_model_name")
                .setParameter("study_instance_uid", studyInstanceUID)
                .setParameter("manufacturer_model_name", manufacturerModelName);

        List results = query.list();
        if(results.isEmpty()){
            return -1;
        } else {
            return (Integer) results.get(0);
        }
    }
}
