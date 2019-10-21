/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.payloadprocessor.models;

import com.abehrdigital.payloadprocessor.utils.Status;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * @author admin
 */
@Entity
@Table(name = "request_routine_execution")
public class RequestRoutineExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @Column(name = "log_text", columnDefinition = "TEXT")
    private String log;
    @Column(name = "request_routine_id")
    private int requestRoutineId;
    @Column(name = "execution_date_time")
    private Timestamp executionDateTime;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "TEXT")
    private Status status;
    @Column(name = "try_number")
    private int tryNumber;

    public RequestRoutineExecution() {
    }

    public RequestRoutineExecution(Integer id, String log,
                                   int requestRoutineId, Timestamp executionDateTime,
                                   Status status, int tryNumber) {
        this.id = id;
        this.log = log;
        this.requestRoutineId = requestRoutineId;
        this.executionDateTime = executionDateTime;
        this.status = status;
        this.tryNumber = tryNumber;
    }

    public RequestRoutineExecution(String log, int requestRoutineId,
                                   Timestamp executionDateTime, Status status, int tryNumber) {
        this.log = log;
        this.requestRoutineId = requestRoutineId;
        this.executionDateTime = executionDateTime;
        this.status = status;
        this.tryNumber = tryNumber;
    }

}
