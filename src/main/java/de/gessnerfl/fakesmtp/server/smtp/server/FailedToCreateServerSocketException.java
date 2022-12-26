package de.gessnerfl.fakesmtp.server.smtp.server;

public class FailedToCreateServerSocketException extends RuntimeException {
    public FailedToCreateServerSocketException(Exception cause) {
        super(cause);
    }
}
