package com.covoiturage.covoiturage.exception;

// 403
public class ForbiddenActionException extends RuntimeException {
    public ForbiddenActionException(String message) {
        super(message);
    }
}
