package com.covoiturage.covoiturage.exception;

// Regle metier 409
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
