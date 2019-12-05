package com.abehrdigital.payloadprocessor.models;

import javax.persistence.*;

/**
 * @author admin
 */
@Entity
@Table(name = "request_details")
public class RequestDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;
    @Column(name = "request_id")
    private int requestId;
    @Column(name = "name")
    private String name;
    @Column(name = "value", columnDefinition="TEXT")
    private String value;

    public RequestDetails() {
    }

    public RequestDetails(Integer id, Integer requestId, String name, String value) {
        this.id = id;
        this.requestId = requestId;
        this.name = name;
        this.value = value;
    }

    public RequestDetails(Integer requestId, String name, String value) {
        this.requestId = requestId;
        this.name = name;
        this.value = value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
