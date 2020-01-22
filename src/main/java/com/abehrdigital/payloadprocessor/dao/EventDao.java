package com.abehrdigital.payloadprocessor.dao;

import com.abehrdigital.payloadprocessor.models.Event;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

import java.util.List;

public class EventDao implements BaseDao<Event, Integer> {
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

    @Override
    public Event get(Integer id) {
        return session.get(Event.class, id);
    }

    @Override
    public void save(Event entity) {
        session.save(entity);
    }

    @Override
    public void update(Event entity) {
        session.update(entity);
    }

    @Override
    public void delete(Event entity) {
        session.delete(entity);
    }

    public Event getWithLock(Integer id, LockMode lockMode) {
        return session.get(
                Event.class, id,
                new LockOptions(lockMode)
        );
    }
}
