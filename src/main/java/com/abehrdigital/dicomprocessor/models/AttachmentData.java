/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor.models;

import com.abehrdigital.dicomprocessor.*;
import java.sql.Blob;
import java.sql.Date;
import javax.persistence.*;

/**
 *
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
    @Column(name = "json_data" , columnDefinition = "LONGTEXT")
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
    
    public AttachmentData(){
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
        
        public Builder(int requestId , String mimeType ,
                String attachmentMnemonic , String attachmentType ,
                String bodySiteSnomedType){
            this.requestId = requestId;
            this.mimeType = mimeType;
            this.attachmentMnemonic = attachmentMnemonic;
            this.attachmentType = attachmentType;
            this.bodySiteSnomedType = bodySiteSnomedType;
        }
        
        public Builder systemOnlyManaged (int val){
            systemOnlyManaged = val;
            return this;
        }
        
        public Builder jsonData (String val){
            jsonData = val;
            return this;
        }
        
        public Builder blobData (Blob val){
            blobData = val;
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

    public String getAttachmentMnemonic() {
        return attachmentMnemonic;
    }

    public String getJson() {
        return jsonData;
    }

    public Blob getBlobData() {
        return blobData;
    }

    public int getId() {
        return id;
    }

    public void setBlobData(Blob value) {
        blobData = value;
    }

    public void setJson(String value) {
        jsonData = value;
    }
}