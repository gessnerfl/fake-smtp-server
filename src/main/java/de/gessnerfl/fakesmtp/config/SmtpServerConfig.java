package de.gessnerfl.fakesmtp.config;

import de.gessnerfl.fakesmtp.smtp.server.SmtpServer;
import org.springframework.context.annotation.Bean;

public interface SmtpServerConfig {
    @Bean
    SmtpServer smtpServer();
}
