package de.gessnerfl.fakesmtp.smtp.server;

public class EmailProcessingException extends RuntimeException {
    public EmailProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
