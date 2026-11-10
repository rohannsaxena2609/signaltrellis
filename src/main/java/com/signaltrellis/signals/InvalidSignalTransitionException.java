package com.signaltrellis.signals;

public class InvalidSignalTransitionException extends RuntimeException {
    public InvalidSignalTransitionException(String message) {
        super(message);
    }
}
