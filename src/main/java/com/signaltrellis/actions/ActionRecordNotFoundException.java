package com.signaltrellis.actions;

public class ActionRecordNotFoundException extends RuntimeException {
    public ActionRecordNotFoundException() {
        super("Action record not found");
    }
}
