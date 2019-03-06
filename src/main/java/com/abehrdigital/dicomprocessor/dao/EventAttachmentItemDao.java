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
                "WHERE attachment_data_id = :attachment_data_id AND " +
                "id NOT IN (SELECT eai.id FROM (SELECT * FROM event_attachment_item) AS eai WHERE eai.attachment_data_id = :attachment_data_id ORDER BY eai.id DESC LIMIT 1) ")
                .setParameter("attachment_data_id", attachmentDataId);

        query.executeUpdate();
    }
}
