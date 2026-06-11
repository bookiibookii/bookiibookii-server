package com.example.bookiibookii.domain.push.sender;

public class InvalidPushTokenException extends RuntimeException {

    public InvalidPushTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}