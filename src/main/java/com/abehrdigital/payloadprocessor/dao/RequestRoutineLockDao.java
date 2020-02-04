package com.abehrdigital.payloadprocessor.dao;

import com.abehrdigital.payloadprocessor.models.RequestQueueLock;
import com.abehrdigital.payloadprocessor.models.RequestRoutineLock;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;

public class RequestRoutineLockDao implements BaseDao<RequestRoutineLock, String> {
    private Session session;

    public RequestRoutineLockDao(Session session) {
        this.session = session;
    }

    @Override
    public RequestRoutineLock get(String routineLock) {
        return session.get(RequestRoutineLock.class, routineLock);
    }

    @Override
    public void save(RequestRoutineLock entity) {
        session.save(entity);
    }

    @Override
    public void update(RequestRoutineLock entity) {
        session.update(entity);
    }

    @Override
    public void delete(RequestRoutineLock entity) {
        session.delete(entity);
    }

    public RequestRoutineLock getWithLock(String requestRoutineUk, LockMode lockMode) {
        return session.get(
                RequestRoutineLock.class, requestRoutineUk,
                new LockOptions(lockMode)
        );
    }
}