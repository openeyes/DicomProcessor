package com.abehrdigital.payloadprocessor.dao;

import com.abehrdigital.payloadprocessor.models.EventAttachmentItem;
import org.hibernate.Session;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
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
        criteriaQuery.where(criteriaBuilder.equal(root.get("attachmentDataId"), attachmentId));
        criteriaQuery.orderBy(criteriaBuilder.asc(root.get("id")));
        return session.createQuery(criteriaQuery).getResultList();
    }

    public List<EventAttachmentItem> getByEventAttachmentGroupId(int eventAttachmentGroupId) {
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
        CriteriaQuery<EventAttachmentItem> criteriaQuery = criteriaBuilder.createQuery(EventAttachmentItem.class);
        Root<EventAttachmentItem> root = criteriaQuery.from(EventAttachmentItem.class);
        criteriaQuery.where(criteriaBuilder.equal(root.get("eventAttachmentGroupId"), eventAttachmentGroupId));
        criteriaQuery.orderBy(criteriaBuilder.asc(root.get("id")));
        return session.createQuery(criteriaQuery).getResultList();
    }
}
