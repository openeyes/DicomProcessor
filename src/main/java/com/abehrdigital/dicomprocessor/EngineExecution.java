package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.utils.Status;

public class EngineExecution {
    private Status status;
    private String log;

    public EngineExecution(Status status, String log) {
        this.status = status;
        this.log = log;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }
}
