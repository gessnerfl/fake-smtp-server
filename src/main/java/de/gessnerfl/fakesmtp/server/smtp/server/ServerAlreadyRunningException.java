package de.gessnerfl.fakesmtp.server.smtp.server;

public class ServerAlreadyRunningException extends RuntimeException {
    public ServerAlreadyRunningException(String message) {
        super(message);
    }
}
