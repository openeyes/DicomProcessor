/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor.utils;

/**
 * @author admin
 */
public enum Status {
    NEW() {
        public Status getExecutionStatus() {
            return Status.FAILED;
        }
    },
    COMPLETE() {
        public Status getExecutionStatus() {
            return Status.COMPLETE;
        }
    },
    SUCCESS() {
        public Status getExecutionStatus() {
            return Status.COMPLETE;
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
    },
    VOID() {
        public Status getExecutionStatus() {
            return Status.FAILED;
        }
    };

    public abstract Status getExecutionStatus();
}
