/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor.models;

import com.abehrdigital.dicomprocessor.utils.Status;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * @author admin
 */
@Entity
@Table(name = "request_routine")
public class RequestRoutine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;
    @Column(name = "request_id")
    private int requestId;
    @Column(name = "execute_request_queue")
    private String executeRequestQueue;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "TEXT")
    private Status status;
    @Column(name = "routine_name")
    private String routineName;
    @Column(name = "try_count")
    private int tryCount;
    @Column(name = "next_try_date_time")
    private Timestamp nextTryDateTime;

    @Column(name = "execute_sequence")
    private int executeSequence;

    public RequestRoutine() {
    }

    public RequestRoutine(Builder builder) {
        this.requestId = builder.requestId;
        this.executeRequestQueue = builder.executeRequestQueue;
        this.status = builder.status;
        this.routineName = builder.routineName;
        this.tryCount = builder.tryCount;
        this.nextTryDateTime = builder.nextTryDateTime;
        this.executeSequence = builder.executeSequence;
    }

    public void successfulExecution() {
        status = Status.COMPLETE;
        tryCount++;
    }

    public void failedExecution() {
        status = Status.FAILED;
        tryCount++;
        setNextTryDateInFiveMinutes();
    }

    public void setExecuteSequence(int value) {
        executeSequence = value;

    }

    public static class Builder {

        //Required

        private final int requestId;
        private final String executeRequestQueue;
        private final String routineName;
        private Status status = Status.NEW;

        private int tryCount = 0;
        private Timestamp nextTryDateTime = null;
        private int executeSequence = 0;

        public Builder(int requestId, String routineName,
                       String executeRequestQueue) {
            this.requestId = requestId;
            this.routineName = routineName;
            this.executeRequestQueue = executeRequestQueue;
        }

        public Builder status(Status value) {
            status = value;
            return this;
        }

        public Builder tryCount(int value) {
            tryCount = value;
            return this;
        }

        public Builder nextTryDateTime(Timestamp timestamp) {
            nextTryDateTime = timestamp;
            return this;
        }

        public Builder executeSequence(int executeSequence) {
            this.executeSequence = executeSequence;
            return this;
        }

        public RequestRoutine build() {
            return new RequestRoutine(this);
        }

    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setNextTryDateInFiveMinutes() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);

        nextTryDateTime = new Timestamp(calendar.getTimeInMillis());
    }

    public String getRoutineName() {
        return routineName;
    }

    public int getRequestId() {
        return requestId;
    }

    public int getExecuteSequence() {
        return executeSequence;
    }

    public Timestamp getNextTryDate() {
        return nextTryDateTime;
    }

    public int getTryCount() {
        return tryCount;
    }

    public int getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public void reset() {
        status = Status.NEW;
        tryCount = 0;
        nextTryDateTime = null;
    }
}
