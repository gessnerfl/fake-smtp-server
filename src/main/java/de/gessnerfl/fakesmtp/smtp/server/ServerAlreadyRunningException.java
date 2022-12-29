package de.gessnerfl.fakesmtp.smtp.server;

public class ServerAlreadyRunningException extends RuntimeException {
    public ServerAlreadyRunningException(String message) {
        super(message);
    }
}
