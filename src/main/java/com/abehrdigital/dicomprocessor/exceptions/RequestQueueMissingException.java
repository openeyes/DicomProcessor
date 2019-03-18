package com.abehrdigital.dicomprocessor.exceptions;

public class RequestQueueMissingException extends Exception {

    public RequestQueueMissingException(String message) {
        super(message);
    }

    public RequestQueueMissingException() {
        super();
    }
}
