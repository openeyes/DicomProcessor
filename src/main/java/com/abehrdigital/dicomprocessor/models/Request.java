/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor.models;

import javax.persistence.*;

/**
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
