package com.abehrdigital.payloadprocessor.exceptions;

public class RequestQueueMissingException extends Exception {

    public RequestQueueMissingException(String message) {
        super(message);
    }

    public RequestQueueMissingException() {
        super();
    }
}
