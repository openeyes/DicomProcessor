package com.abehrdigital.payloadprocessor.exceptions;

public class EmptyKnownFieldsException extends Throwable {
    public EmptyKnownFieldsException(String message) { super(message); }
    public EmptyKnownFieldsException() { super(); }
}
