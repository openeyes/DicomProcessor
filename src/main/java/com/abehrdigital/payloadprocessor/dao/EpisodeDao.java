package com.abehrdigital.payloadprocessor.dao;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

import java.util.List;

public class EpisodeDao {
    private Session session;
    public EpisodeDao(Session session) {
        this.session = session;
    }

    public List getByPatientId(int patientId) {
        NativeQuery query = session.createSQLQuery("" +
                "SELECT *" +
                " FROM episode ep " +
                "WHERE ep.patient_id = :patientId ")
                .setParameter("patientId", patientId);
        return query.getResultList();
    }
}
