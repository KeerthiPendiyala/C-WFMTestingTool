package com.ukgqtm.identity.api;

public class AuthenticationDeniedException extends RuntimeException {
    public AuthenticationDeniedException(String message) {
        super(message);
    }
}
