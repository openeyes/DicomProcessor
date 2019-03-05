package com.abehrdigital.dicomprocessor.dao;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

public class EventAttachmentItemDao {
    private Session session;

    public EventAttachmentItemDao(Session session) {
        this.session = session;
    }

    public void deleteByAttachmentDataId(int attachmentDataId){
        NativeQuery query = session.createSQLQuery("" +
                "DELETE FROM event_attachment_item " +
                "WHERE event_attachment_item.attachment_data_id = :attachment_data_id ")
                .setParameter("attachment_data_id", attachmentDataId);
        query.executeUpdate();
    }
}
