package com.adaptiveos.service;

/** The request was well-formed JSON but semantically invalid (maps to HTTP 400). */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
