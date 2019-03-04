/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor.models;

import javax.persistence.*;
import java.sql.Blob;

/**
 * @author admin
 */
@Entity
@Table(name = "attachment_data")
public class AttachmentData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @Column(name = "request_id")
    private Integer requestId;
    @Column(name = "blob_data")
    private Blob blobData;
    @Column(name = "thumbnail_small_blob" ,columnDefinition = "MEDIUMBLOB")
    private Blob smallThumbnail;
    @Column(name = "thumbnail_medium_blob" , columnDefinition = "MEDIUMBLOB")
    private Blob mediumThumbnail;
    @Column(name = "thumbnail_large_blob" , columnDefinition = "MEDIUMBLOB")
    private Blob largeThumbnail;
    @Column(name = "json_data", columnDefinition = "LONGTEXT")
    private String jsonData;
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
        private String jsonData = null;
        private Blob blobData = null;

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

        public Builder jsonData(String value) {
            if (value.equals("")) {
                jsonData = null;
            } else {
                jsonData = value;
            }
            return this;
        }

        public Builder blobData(Blob value) {
            blobData = value;
            return this;
        }

        public AttachmentData build() {
            return new AttachmentData(this);
        }
    }

    public AttachmentData(Builder builder) {
        requestId = builder.requestId;
        blobData = builder.blobData;
        jsonData = builder.jsonData;
        attachmentType = builder.attachmentType;
        bodySiteSnomedType = builder.bodySiteSnomedType;
        mimeType = builder.mimeType;
        attachmentMnemonic = builder.attachmentMnemonic;
        systemOnlyManaged = builder.systemOnlyManaged;
    }

    public String getJsonData() {
        return jsonData;
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

    public String getJson() {
        return jsonData;
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

    public void setJson(String value) {
        if (value.equals("")) {
            jsonData = null;
        } else {
            jsonData = value;
        }
    }
}
