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
@Table(name = "routine_library")
public class RoutineLibrary {
    @Id
    @Column(name = "routine_name")
    private String routineName;
    @Column(name = "routine_body", columnDefinition = "TEXT")
    private String routineBody;
    @Column(name = "hash_code", columnDefinition = "BIGINT")
    private Integer hashCode;

    public RoutineLibrary() {
    }

    public RoutineLibrary(String routineName, String routineBody, Integer hashCode) {
        this.routineName = routineName;
        this.routineBody = routineBody;
        this.hashCode = hashCode;
    }

    public RoutineLibrary(String routineName, String routineBody) {
        this.routineName = routineName;
        this.routineBody = routineBody;
    }

    public String getRoutineBody() {
        return routineBody;
    }
}
