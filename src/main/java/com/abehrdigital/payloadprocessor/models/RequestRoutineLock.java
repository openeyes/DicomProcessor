package com.abehrdigital.payloadprocessor.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author admin
 */
@Entity
@Table(name = "request_routine_lock")
public class RequestRoutineLock {
    @Id
    @Column(name = "routine_lock")
    private String routineLock;

    public RequestRoutineLock() {
    }

    public RequestRoutineLock(String routineLock) {
        this.routineLock = routineLock;
    }

    public void setRoutineLock(String routineLock) {
        this.routineLock = routineLock;
    }

    public String getRoutineLock() {
        return routineLock;
    }
}