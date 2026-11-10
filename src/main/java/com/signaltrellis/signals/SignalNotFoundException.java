package com.signaltrellis.signals;

public class SignalNotFoundException extends RuntimeException {
    public SignalNotFoundException() {
        super("Signal not found");
    }
}
