/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author admin
 */
@Entity
@Table(name = "request_queue_lock")
public class RequestQueueLock {
    @Id
    @Column(name = "request_queue")
    private String requestQueue;

    public RequestQueueLock() {
    }

    public RequestQueueLock(String requestQueue) {
        this.requestQueue = requestQueue;
    }

    public void setRequestQueue(String requestQueue) {
        this.requestQueue = requestQueue;
    }

    public String getRequestQueue() {
        return requestQueue;
    }
}