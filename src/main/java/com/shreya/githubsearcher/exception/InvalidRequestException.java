package com.shreya.githubsearcher.exception;

/** The client sent a request we cannot serve (maps to HTTP 400). */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
