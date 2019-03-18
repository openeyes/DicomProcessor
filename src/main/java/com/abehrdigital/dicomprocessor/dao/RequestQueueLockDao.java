/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor.dao;

import com.abehrdigital.dicomprocessor.models.RequestQueueLock;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;

/**
 * @author admin
 */
public class RequestQueueLockDao implements BaseDao<RequestQueueLock, String> {
    private Session session;

    public RequestQueueLockDao(Session session) {
        this.session = session;
    }

    public RequestQueueLock getWithLock(String queueName, LockMode lockMode) {
        return session.get(
                RequestQueueLock.class, queueName,
                new LockOptions(lockMode)
        );
    }

    @Override
    public RequestQueueLock get(String id) {
        return session.get(RequestQueueLock.class, id);
    }

    @Override
    public void save(RequestQueueLock entity) {
        session.save(entity);
    }

    @Override
    public void update(RequestQueueLock entity) {
        session.update(entity);
    }

    @Override
    public void delete(RequestQueueLock entity) {
        session.delete(entity);
    }

}
