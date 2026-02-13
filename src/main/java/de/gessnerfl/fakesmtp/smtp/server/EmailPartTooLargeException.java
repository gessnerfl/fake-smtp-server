package de.gessnerfl.fakesmtp.smtp.server;

public class EmailPartTooLargeException extends RuntimeException {
    public EmailPartTooLargeException(String message) {
        super(message);
    }
}
