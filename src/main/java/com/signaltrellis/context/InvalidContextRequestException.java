package com.signaltrellis.context;

public class InvalidContextRequestException extends RuntimeException {
    public InvalidContextRequestException(String message) {
        super(message);
    }
}
