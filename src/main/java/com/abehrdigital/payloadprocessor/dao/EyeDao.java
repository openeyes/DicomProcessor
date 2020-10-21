package com.abehrdigital.payloadprocessor.dao;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

public class EyeDao {
    private Session session;

    public EyeDao(Session session) {
        this.session = session;
    }

    public int getIdByLaterality(String laterality) {
        NativeQuery query = session.createSQLQuery("" +
                "SELECT e.id FROM eye e " +
                "WHERE LEFT(e.name, 1) = :name ")
                .setParameter("name", laterality);

        return (int) query.getSingleResult();
    }
}