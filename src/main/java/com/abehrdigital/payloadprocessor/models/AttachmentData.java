/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.payloadprocessor.models;

import javax.persistence.*;
import java.sql.Blob;

/**
 * @author admin
 */
@Entity
@Table(name = "attachment_data")
@NamedNativeQueries({
        @NamedNativeQuery(name = "attachmentForEventWithSameHashcode", query = "" +
                "SELECT * FROM attachment_data t " +
                " JOIN `event_attachment_item` eti ON eti.`attachment_data_id` = t.id " +
                "JOIN event_attachment_group eag ON eti.`event_attachment_group_id` = eag.id " +
                "JOIN `event` ev ON ev.id = eag.event_id " +
                "WHERE t.hash_code = :hash_code AND ev.id = :event_id",
                resultClass = AttachmentData.class),
        @NamedNativeQuery(name = "attachmentUsedInEventAttachmentItem", query = "" +
                "SELECT * FROM attachment_data t " +
                "JOIN `event_attachment_item` eti ON eti.`attachment_data_id` = t.id " +
                "WHERE eti.attachment_data_id = :attachment_data_id ",
                resultClass = AttachmentData.class),
        @NamedNativeQuery(name = "attachmentsThatAreAttachedWithSameHashcode", query = "" +
                "SELECT * FROM attachment_data t " +
                "JOIN `event_attachment_item` eti ON eti.`attachment_data_id` = t.id " +
                "JOIN event_attachment_group eag ON eag.id = eti.event_attachment_group_id " +
                "JOIN event ev ON ev.id = eag.event_id " +
                "WHERE t.hash_code = :hash_code AND t.id != :current_attachment_id AND ev.deleted = 0",
                resultClass = AttachmentData.class)
})
public class AttachmentData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @Column(name = "request_id")
    private Integer requestId;
    @Column(name = "blob_data")
    private Blob blobData;
    @Column(name = "thumbnail_small_blob", columnDefinition="mediumblob")
    private Blob smallThumbnail;
    @Column(name = "thumbnail_medium_blob", columnDefinition="mediumblob")
    private Blob mediumThumbnail;
    @Column(name = "thumbnail_large_blob", columnDefinition="mediumblob")
    private Blob largeThumbnail;
    @Column(name = "text_data", columnDefinition = "LONGTEXT")
    private String textData;
    @Column(name = "attachment_type")
    private String attachmentType;
    @Column(name = "body_site_snomed_type")
    private String bodySiteSnomedType;
    @Column(name = "mime_type")
    private String mimeType;
    @Column(name = "attachment_mnemonic")
    private String attachmentMnemonic;
    @Column(name = "system_only_managed")
    private int systemOnlyManaged;
    @Column(name = "hash_code", columnDefinition = "BIGINT")
    private Integer hashCode;

    public AttachmentData() {
    }

    public static class Builder {
        //Required
        private final int requestId;
        private final String mimeType;
        private final String attachmentMnemonic;
        private final String attachmentType;
        private final String bodySiteSnomedType;


        private int systemOnlyManaged = 0;
        private String textData = null;
        private Blob blobData = null;
        private Integer hashCode = null;

        public Builder(int requestId, String mimeType,
                       String attachmentMnemonic, String attachmentType,
                       String bodySiteSnomedType) {
            this.requestId = requestId;
            this.mimeType = mimeType;
            this.attachmentMnemonic = attachmentMnemonic;
            this.attachmentType = attachmentType;
            this.bodySiteSnomedType = bodySiteSnomedType;
        }

        public Builder systemOnlyManaged(int value) {
            systemOnlyManaged = value;
            return this;
        }

        public Builder textData(String value) {
            if (value.equals("")) {
                textData = null;
            } else {
                textData = value;
            }
            return this;
        }

        public Builder blobData(Blob value) {
            blobData = value;
            return this;
        }

        public Builder hashCode(int value) {
            hashCode = value;
            return this;
        }

        public AttachmentData build() {
            return new AttachmentData(this);
        }
    }

    public AttachmentData(Builder builder) {
        requestId = builder.requestId;
        blobData = builder.blobData;
        textData = builder.textData;
        attachmentType = builder.attachmentType;
        bodySiteSnomedType = builder.bodySiteSnomedType;
        mimeType = builder.mimeType;
        attachmentMnemonic = builder.attachmentMnemonic;
        systemOnlyManaged = builder.systemOnlyManaged;
        hashCode = builder.hashCode;
    }

    public String getTextData() {
        return textData;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public String getBodySiteSnomedType() {
        return bodySiteSnomedType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public int getId() {
        return id;
    }

    public String getAttachmentMnemonic() {
        return attachmentMnemonic;
    }

    public Blob getBlobData() {
        return blobData;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public Blob getSmallThumbnail() {
        return smallThumbnail;
    }

    public Blob getMediumThumbnail() {
        return mediumThumbnail;
    }

    public Blob getLargeThumbnail() {
        return largeThumbnail;
    }

    public void setSmallThumbnail(Blob smallThumbnail) {
        this.smallThumbnail = smallThumbnail;
    }

    public void setMediumThumbnail(Blob mediumThumbnail) {
        this.mediumThumbnail = mediumThumbnail;
    }

    public void setLargeThumbnail(Blob largeThumbnail) {
        this.largeThumbnail = largeThumbnail;
    }

    public void setBlobData(Blob value) {
        blobData = value;
    }

    public void setTextData(String value) {
        if (value.equals("")) {
            textData = null;
        } else {
            textData = value;
        }
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public int getHashCode() {
        return hashCode;
    }

    public void setHashCode(Integer hashCode) {
        this.hashCode = hashCode;
    }
}
