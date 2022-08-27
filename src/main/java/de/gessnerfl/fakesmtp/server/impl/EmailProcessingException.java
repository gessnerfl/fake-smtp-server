package de.gessnerfl.fakesmtp.server.impl;

public class EmailProcessingException extends RuntimeException {
    public EmailProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
