package com.abehrdigital.dicomprocessor.dao;

import com.abehrdigital.dicomprocessor.models.RequestRoutineExecution;
import org.hibernate.Session;

public class RequestRoutineExecutionDao implements BaseDao<RequestRoutineExecution, Integer> {
    private Session session;

    public RequestRoutineExecutionDao(Session session) {
        this.session = session;
    }

    @Override
    public RequestRoutineExecution get(Integer primaryKey) {
        return session.get(RequestRoutineExecution.class, primaryKey);
    }

    @Override
    public void save(RequestRoutineExecution entity) {
        session.save(entity);
    }

    @Override
    public void update(RequestRoutineExecution entity) {
        session.update(entity);
    }

    @Override
    public void delete(RequestRoutineExecution entity) {
        session.delete(entity);
    }
}
