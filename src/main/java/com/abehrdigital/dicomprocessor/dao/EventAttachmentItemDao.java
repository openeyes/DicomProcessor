package com.abehrdigital.dicomprocessor.dao;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

import java.math.BigInteger;

public class EventAttachmentItemDao {
    private Session session;

    public EventAttachmentItemDao(Session session) {
        this.session = session;
    }

    public void deleteByAttachmentDataId(int attachmentDataId) {
        NativeQuery query = session.createSQLQuery("" +
                "SELECT COUNT(id) FROM `event_attachment_item` " +
                "WHERE `attachment_data_id`= :attachment_data_id")
                .setParameter("attachment_data_id", attachmentDataId);
        int numberRecordsForAttachmentDataId = ((BigInteger) query.list().get(0)).intValue();

        if (numberRecordsForAttachmentDataId > 1) {
            System.err.println(">1");
            query = session.createSQLQuery("" +
                    "DELETE FROM event_attachment_item " +
                    "WHERE attachment_data_id = :attachment_data_id AND " +
                    "id != ( SELECT * FROM ( " +
                    "SELECT MAX(id) FROM event_attachment_item " +
                    " WHERE attachment_data_id = :attachment_data_id" +
                    " ) AS eai ) ")
                    .setParameter("attachment_data_id", attachmentDataId);
        } else {
            System.err.println("==1");
            query = session.createSQLQuery("" +
                    "DELETE FROM event_attachment_item " +
                    "WHERE event_attachment_item.attachment_data_id = :attachment_data_id ")
                    .setParameter("attachment_data_id", attachmentDataId);
        }
        query.executeUpdate();
    }
}
