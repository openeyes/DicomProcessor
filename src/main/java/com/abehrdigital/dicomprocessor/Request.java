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

    public Request() {
    }

    public Request(Integer id, String requestType, String systemMessage) {
        this.id = id;
        this.requestType = requestType;
        this.systemMessage = systemMessage;
    }
}
