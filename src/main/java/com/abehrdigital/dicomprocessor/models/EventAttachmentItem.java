package com.abehrdigital.dicomprocessor.models;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "event_attachment_item")
public class EventAttachmentItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;
    @Column(name = "event_attachment_group_id")
    private int eventAttachmentGroupId;
    @Column(name = "attachment_data_id")
    private int attachmentDataId;
    @Column(name = "system_only_managed")
    private int systemOnlyManaged;
    @Column(name = "event_document_view_set")
    private String eventDocumentViewSet;
    @Column(name = "last_modified_user_id")
    private int lastModifiedUserId;
    @Column(name = "last_modified_date")
    private Timestamp lastModifiedDate;
    @Column(name = "created_user_id")
    private int createdUserId;
    @Column(name = "created_date")
    private Timestamp createdDate;

    public EventAttachmentItem() {
        //Empty
    }

    public int getEventAttachmentGroupId() {
        return eventAttachmentGroupId;
    }
}
