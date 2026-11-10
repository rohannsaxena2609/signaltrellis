package com.signaltrellis.actions;

public class InvalidActionRequestException extends RuntimeException {
    public InvalidActionRequestException(String message) {
        super(message);
    }
}
