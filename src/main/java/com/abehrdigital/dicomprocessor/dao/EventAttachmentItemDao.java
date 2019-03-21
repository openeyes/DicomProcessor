package com.abehrdigital.dicomprocessor.dao;

import com.abehrdigital.dicomprocessor.models.EventAttachmentItem;
import com.abehrdigital.dicomprocessor.models.RequestRoutine;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.math.BigInteger;
import java.util.List;

public class EventAttachmentItemDao {
    private Session session;

    public EventAttachmentItemDao(Session session) {
        this.session = session;
    }

    public void delete(EventAttachmentItem eventAttachmentItem) {
        session.delete(eventAttachmentItem);
    }

    public List<EventAttachmentItem> getByAttachmentDataId(int attachmentId) {
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
        CriteriaQuery<EventAttachmentItem> criteriaQuery = criteriaBuilder.createQuery(EventAttachmentItem.class);
        Root<EventAttachmentItem> root = criteriaQuery.from(EventAttachmentItem.class);
        criteriaQuery.where(criteriaBuilder.equal(root.get("attachment_data_id"), attachmentId));
        criteriaQuery.orderBy(criteriaBuilder.asc(root.get("id")));
        return session.createQuery(criteriaQuery).getResultList();
    }

    public List<EventAttachmentItem> getByEventAttachmentGroupId(int eventAttachmentGroupId) {
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
        CriteriaQuery<EventAttachmentItem> criteriaQuery = criteriaBuilder.createQuery(EventAttachmentItem.class);
        Root<EventAttachmentItem> root = criteriaQuery.from(EventAttachmentItem.class);
        criteriaQuery.where(criteriaBuilder.equal(root.get("event_attachment_group_id"), eventAttachmentGroupId));
        criteriaQuery.orderBy(criteriaBuilder.asc(root.get("id")));
        return session.createQuery(criteriaQuery).getResultList();
    }
}
