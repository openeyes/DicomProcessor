/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.payloadprocessor.dao;

import com.abehrdigital.payloadprocessor.models.AttachmentData;
import com.abehrdigital.payloadprocessor.utils.QueryResultUtils;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

/**
 * @author admin
 */
public class AttachmentDataDao implements BaseDao<AttachmentData, Integer> {

    private Session session;

    public AttachmentDataDao(Session session) {
        this.session = session;
    }

    @Override
    public AttachmentData get(Integer id) {
        return session.get(AttachmentData.class, id);
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

    public AttachmentData getByAttachmentMnemonicAndBodySite(String attachmentMnemonic, String bodySite, int requestId) {
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
        CriteriaQuery<AttachmentData> criteriaQuery = criteriaBuilder.createQuery(AttachmentData.class);
        Root root = criteriaQuery.from(AttachmentData.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("requestId"), requestId)));

        if (attachmentMnemonic != null) {
            predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("attachmentMnemonic"), attachmentMnemonic)));
        }
        if (bodySite != null) {
            predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("bodySiteSnomedType"), bodySite)));
        }
        criteriaQuery.where(predicates.toArray(new Predicate[0]));

        return (AttachmentData) QueryResultUtils.getFirstResultOrNull(session.createQuery(criteriaQuery));
    }

    public boolean attachmentAlreadyExistsWithThisHashCodeAndEventId(int eventId , int hashCode) throws Exception {

        NativeQuery query = session.createSQLQuery("" +
                "SELECT DISTINCT t.id FROM attachment_data t " +
                "RIGHT JOIN `event_attachment_item` eti ON eti.`attachment_data_id` = t.id " +
                "RIGHT JOIN event_attachment_group eag ON eag.`event_id` = :event_id " +
                "WHERE t.hash_code = :hash_code ")
                .setParameter("event_id", eventId)
                .setParameter("hash_code", hashCode);

        List results = query.list();
        return !results.isEmpty();
    }
}
