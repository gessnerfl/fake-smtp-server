package de.gessnerfl.fakesmtp.smtp.server;

public class FailedToCreateServerSocketException extends RuntimeException {
    public FailedToCreateServerSocketException(Exception cause) {
        super(cause);
    }
}
