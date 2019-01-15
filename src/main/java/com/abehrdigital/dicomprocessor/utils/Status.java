/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor.utils;

/**
 *
 * @author admin
 */
public enum Status {
    NEW() {
        public Status getExecutionStatus() {
            return Status.FAILED;
        }
    },
    SUCCESS() {
        public Status getExecutionStatus() {
            return Status.SUCCESS;
        }
    },
    FAILED() {
        public Status getExecutionStatus() {
            return Status.FAILED;
        }
    },
    RETRY() {
        public Status getExecutionStatus() {
            return Status.FAILED;
        }
    };

    public abstract Status getExecutionStatus();
}
