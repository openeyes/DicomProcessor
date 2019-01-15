/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.models.AttachmentData;
import com.abehrdigital.dicomprocessor.models.RequestQueue;
import com.abehrdigital.dicomprocessor.models.RequestQueueLock;
import org.hibernate.Session;

/**
 *
 * @author admin
 */
public class AttachmentDataDaoImpl implements BaseDao<AttachmentData, Integer> {

    private Session session;

    public AttachmentDataDaoImpl(Session session) {
        this.session = session;
    }

    @Override
    public AttachmentData get(Integer id) {
        return (AttachmentData) session.get(AttachmentData.class, id);
    }

    @Override
    public void save(AttachmentData entity) {
        session.save(entity);
    }

    @Override
    public void update(AttachmentData entity) {
        session.update(entity);
    }

    @Override
    public void delete(AttachmentData entity) {
        session.delete(entity);
    }

}
