/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.models.*;
import com.abehrdigital.dicomprocessor.utils.Status;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author admin
 */
@Entity
@Table(name = "request")
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;
    @Column(name = "request_type")
    private String requestType;
    @Column(name = "system_message")
    private String systemMessage;
    @Transient
    private Session databaseConnection;

    public Request() {
    }

    public Request(Integer id, String requestType, String systemMessage) {
        this.id = id;
        this.requestType = requestType;
        this.systemMessage = systemMessage;
    }

    public void setSession(Session session) {
        this.databaseConnection = session;
    }

    public DicomParser getDicom(String attachmentMnemonic, String bodySite) throws HibernateException {
        return new DicomParser(getBlob(attachmentMnemonic, bodySite));
    }

    public Blob getBlob(String attachmentMnemonic, String bodySite) throws HibernateException {
        return getData(attachmentMnemonic, bodySite).getBlobData();
    }

    public String getJson(String attachmentMnemonic, String bodySite) throws HibernateException {
        AttachmentData data = getData(attachmentMnemonic, bodySite);
        if (data != null && data.getJson() != null) {
            return data.getJson();
        } else {
            return "{}";
        }

    }

    public AttachmentData getData(String attachmentMnemonic, String bodySite) throws HibernateException {
        Criteria criteria = databaseConnection.createCriteria(AttachmentData.class);

        if (attachmentMnemonic != null) {
            criteria.add(Restrictions.eq("attachmentMnemonic", attachmentMnemonic));
        }

        if (bodySite != null) {
            criteria.add(Restrictions.eq("bodySiteSnomedType", bodySite));
        }
        
        return (AttachmentData) criteria.uniqueResult();
    }

    public void putJson(
            String attachmentMnemonic,
            String json,
            String attachmentType,
            String bodySite,
            String mimeType) throws SQLException {
        AttachmentData attachmentData = getData(attachmentMnemonic, bodySite);

        if (attachmentData != null) {
            attachmentData.setJson(json);
            databaseConnection.save(attachmentData);
        } else {
            attachmentData = new AttachmentData.Builder(
                    id, mimeType,
                    attachmentMnemonic, attachmentType,
                    bodySite)
                    .jsonData(json)
                    .build();
            databaseConnection.save(attachmentData);
        }
    }

    public void addRoutine(String routineName) throws HibernateException {
        RequestRoutine requestRoutine = getRequestRoutine(routineName);
        if (requestRoutine != null) {
            resetRequestRoutine(requestRoutine);
        } else {
            if (routineInLibraryExists(routineName)) {
                requestRoutine = new RequestRoutine.Builder(id ,
                        routineName , "dicom_queue").build();
                      

                databaseConnection.save(requestRoutine);
            } else {
                throw new HibernateException("Routine in the library doesn't exist");
            }
        }
    }

    private boolean routineInLibraryExists(String routineName) throws HibernateException {
        return (databaseConnection.get(RoutineLibrary.class, routineName) != null);
    }

    private RequestRoutine getRequestRoutine(String routineName) throws HibernateException {
        Criteria criteria = databaseConnection.createCriteria(RequestRoutine.class);
        criteria.add(Restrictions.eq("routineName", routineName));
        criteria.add(Restrictions.eq("requestId", id));

        return (RequestRoutine) criteria.uniqueResult();
    }

    private void resetRequestRoutine(RequestRoutine requestRoutine) throws HibernateException {
        requestRoutine.reset();
        databaseConnection.save(requestRoutine);
    }

    public void putPdf(
            String attachmentMnemonic,
            Blob pdfBlob,
            String attachmentType,
            String bodySite,
            String mimeType
    ) throws HibernateException {
        AttachmentData attachmentData = getData(attachmentMnemonic, bodySite);
        if (attachmentData != null) {
            attachmentData.setBlobData(pdfBlob);
            databaseConnection.save(attachmentData);
        } else {
            attachmentData = new AttachmentData.Builder(
                    id, mimeType, attachmentMnemonic,
                    attachmentType, bodySite)
                    .blobData(pdfBlob).build();
            databaseConnection.save(attachmentData);
        }
    }

}
