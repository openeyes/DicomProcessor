package com.abehrdigital.payloadprocessor.dao;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

import java.util.List;

public class BiometryImportedEventsDao {

    private Session session;

    public BiometryImportedEventsDao(Session session) {
        this.session = session;
    }

    public Integer getEventIdByStudyId(String studyInstanceUID){
        NativeQuery query = session.createSQLQuery("" +
                "SELECT bie.event_id" +
                " FROM ophinbiometry_imported_events bie " +
                "WHERE bie.study_id = :study_instance_uid ")
                .setParameter("study_instance_uid", studyInstanceUID);

        List results = query.list();
        if(results.isEmpty()){
            return -1;
        } else {
            return (Integer) results.get(0);
        }
    }
}
