package com.signaltrellis.context;

public class ContextRecordNotFoundException extends RuntimeException {
    public ContextRecordNotFoundException() {
        super("Context record not found");
    }
}
