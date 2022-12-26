package de.gessnerfl.fakesmtp.server.smtp.server;

public class UnexpectedSMTPServerException extends RuntimeException {
    public UnexpectedSMTPServerException(String message, Exception parent) {
        super(message, parent);
    }
}
