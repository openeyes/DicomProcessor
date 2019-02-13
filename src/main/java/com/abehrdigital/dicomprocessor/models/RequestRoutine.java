/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor.models;

import com.abehrdigital.dicomprocessor.utils.RequestRoutineNextTryTimeCalculator;
import com.abehrdigital.dicomprocessor.utils.Status;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * @author admin
 */
@Entity
@Table(name = "request_routine")
@OptimisticLocking(type = OptimisticLockType.DIRTY)
@DynamicUpdate
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
        nextTryDateTime = null;
        tryCount++;
    }

    public void failedExecution() {
        tryCount++;
        calculateAndSetNextTryDateAfterFailure();
        if (nextTryDateTime == null) {
            status = Status.FAILED;
        } else {
            status = Status.RETRY;
        }
    }

    public void setExecuteSequence(int value) {
        executeSequence = value;

    }

    public void
    updateFieldsByStatus(Status routineStatus) {
        if (routineStatus == Status.COMPLETE) {
            successfulExecution();
        } else if (routineStatus == Status.FAILED) {
            failedExecution();
        } else {
            setStatus(routineStatus);
        }
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

    public void calculateAndSetNextTryDateAfterFailure() {
        nextTryDateTime = RequestRoutineNextTryTimeCalculator.getNextTryDateTimeIntervalInSeconds(tryCount);
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
