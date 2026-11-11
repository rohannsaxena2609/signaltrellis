package com.signaltrellis.assistant;

public class InvalidAssistantRequestException extends RuntimeException {
    public InvalidAssistantRequestException(String message) {
        super(message);
    }
}
