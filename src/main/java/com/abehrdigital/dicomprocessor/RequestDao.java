package com.abehrdigital.dicomprocessor;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;

public class RequestDao implements BaseDao<Request, Integer> {
    private Session session;

    public RequestDao(Session session) {
        this.session = session;
    }

    @Override
    public Request get(Integer id) {
        return session.get(Request.class, id);
    }

    @Override
    public void save(Request entity) {
        session.save(entity);
    }

    @Override
    public void update(Request entity) {
        session.update(entity);
    }

    @Override
    public void delete(Request entity) {
        session.delete(entity);
    }

    public Request getWithLock(int requestId, LockMode lockMode) {
        return session.get(
                Request.class, requestId,
                new LockOptions(lockMode)
        );
    }
}
