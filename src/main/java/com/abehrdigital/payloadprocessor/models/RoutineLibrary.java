/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.payloadprocessor.models;

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
    @Column(name = "hash_code", columnDefinition = "BIGINT")
    private Integer hashCode;

    public RoutineLibrary() {
    }

    public RoutineLibrary(String routineName, Integer hashCode) {
        this.routineName = routineName;
        this.hashCode = hashCode;
    }

    public RoutineLibrary(String routineName) {
        this.routineName = routineName;
    }
}
