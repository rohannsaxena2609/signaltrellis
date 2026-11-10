package com.signaltrellis.signals;

public class InvalidSignalRequestException extends RuntimeException {
    public InvalidSignalRequestException(String message) {
        super(message);
    }
}
