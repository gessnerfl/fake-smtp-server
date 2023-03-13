package de.gessnerfl.fakesmtp.smtp.server;

public interface SmtpServer {
    void start();
    void stop();
    int getPort();
}
