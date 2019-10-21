/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.payloadprocessor.dao;

import com.abehrdigital.payloadprocessor.models.RequestQueue;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;

/**
 * @author admin
 */
public class RequestQueueDao implements BaseDao<RequestQueue, String> {
    private Session session;

    public RequestQueueDao(Session session) {
        this.session = session;
    }

    public RequestQueue getWithLock(String id, LockMode lockMode) {
        session.clear();
        return session.get(
                RequestQueue.class, id,
                new LockOptions(lockMode)
        );
    }

    @Override
    public RequestQueue get(String id) {
        return session.get(RequestQueue.class, id);
    }

    @Override
    public void save(RequestQueue entity) {
        session.save(entity);
    }

    @Override
    public void update(RequestQueue entity) {
        session.update(entity);
    }

    @Override
    public void delete(RequestQueue entity) {
        session.delete(entity);
    }

}
