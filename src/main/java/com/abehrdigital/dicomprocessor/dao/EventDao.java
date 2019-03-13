package com.abehrdigital.dicomprocessor.dao;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

import java.util.List;

public class EventDao {
    private Session session;
    public EventDao(Session session) {
        this.session = session;
    }

    public List getNotDeleted(int eventId) {
        int EVENT_NOT_DELETED = 0;
        NativeQuery query = session.createSQLQuery("" +
                "SELECT *" +
                " FROM event ev " +
                "WHERE ev.id = :eventId " +
                "AND ev.deleted = :deleted ")
                .setParameter("eventId", eventId)
                .setParameter("deleted" , EVENT_NOT_DELETED);
        return query.getResultList();
    }
}
