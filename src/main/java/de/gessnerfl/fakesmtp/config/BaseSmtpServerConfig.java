package de.gessnerfl.fakesmtp.config;

import de.gessnerfl.fakesmtp.smtp.auth.BasicUsernamePasswordValidator;
import de.gessnerfl.fakesmtp.smtp.auth.EasyAuthenticationHandlerFactory;
import de.gessnerfl.fakesmtp.smtp.command.CommandHandler;
import de.gessnerfl.fakesmtp.smtp.server.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.List;

@Profile("default")
@Configuration
public class BaseSmtpServerConfig implements SmtpServerConfig {

    private final BuildProperties buildProperties;
    private final ResourceLoader resourceLoader;
    private final FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;
    private final List<MessageListener> messageListeners;
    private final BasicUsernamePasswordValidator basicUsernamePasswordValidator;
    private final CommandHandler commandHandler;
    private final boolean virtualThreadsEnabled;
    private final Logger logger;

    @Autowired
    public BaseSmtpServerConfig(BuildProperties buildProperties,
                                ResourceLoader resourceLoader,
                                FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties,
                                List<MessageListener> messageListeners,
                                BasicUsernamePasswordValidator basicUsernamePasswordValidator,
                                CommandHandler commandHandler,
                                @Value("${spring.threads.virtual.enabled:false}") boolean virtualThreadsEnabled,
                                Logger logger) {
        this.buildProperties = buildProperties;
        this.resourceLoader = resourceLoader;
        this.fakeSmtpConfigurationProperties = fakeSmtpConfigurationProperties;
        this.messageListeners = messageListeners;
        this.basicUsernamePasswordValidator = basicUsernamePasswordValidator;
        this.commandHandler = commandHandler;
        this.virtualThreadsEnabled = virtualThreadsEnabled;
        this.logger = logger;
    }

    @Override
    @Bean
    public SmtpServer smtpServer() {
        BaseSmtpServer smtpServer = createBaseSmtpServerFor(new MessageListenerAdapter(messageListeners), sessionIdFactory());
        smtpServer.setPort(fakeSmtpConfigurationProperties.getPort());
        smtpServer.setBindAddress(fakeSmtpConfigurationProperties.getBindAddress());
        if (fakeSmtpConfigurationProperties.getAuthentication() != null) {
            configureAuthentication(smtpServer, fakeSmtpConfigurationProperties.getAuthentication());
        }
        if (fakeSmtpConfigurationProperties.getMaxMessageSize() != null){
            smtpServer.setMaxMessageSizeInBytes(fakeSmtpConfigurationProperties.getMaxMessageSize().toBytes());
        }
        if(fakeSmtpConfigurationProperties.isRequireTLS() && fakeSmtpConfigurationProperties.getTlsKeystore() == null){
            throw new IllegalArgumentException("SMTP server TLS keystore configuration is missing");
        }
        smtpServer.setRequireTLS(fakeSmtpConfigurationProperties.isRequireTLS());
        smtpServer.setEnableTLS(fakeSmtpConfigurationProperties.isRequireTLS());

        var tlsKeystoreConfig = fakeSmtpConfigurationProperties.getTlsKeystore();
        if (tlsKeystoreConfig != null) {
            logger.info("Setup TLS keystore of SMTP server");
            var keyStoreFileStream = resourceLoader.getResource(tlsKeystoreConfig.getLocation());
            var keyStorePassphrase =tlsKeystoreConfig.getPassword().toCharArray();
            try {
                var keyStore = KeyStore.getInstance(tlsKeystoreConfig.getType().name());
                keyStore.load(keyStoreFileStream.getInputStream(), keyStorePassphrase);

                var kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(keyStore, keyStorePassphrase);

                var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init((KeyStore) null);

                var sslContext = SSLContext.getInstance("TLS");
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

                smtpServer.setSslContext(sslContext);
                logger.info("Setup of TLS keystore of SMTP server completed");
            } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException |
                     UnrecoverableKeyException | KeyManagementException e) {
                throw new IllegalStateException("Failed to setup TLS keystore of SMTP server");
            }
        }

        return smtpServer;
    }

    @Bean
    public SessionIdFactory sessionIdFactory(){
        return new TimeBasedSessionIdFactory();
    }

    BaseSmtpServer createBaseSmtpServerFor(MessageListenerAdapter messageListenerAdapter, SessionIdFactory sessionIdFactory){
        final var softwareName = "FakeSMTPServer " + buildProperties.getVersion();
        return new BaseSmtpServer(softwareName, messageListenerAdapter, commandHandler, sessionIdFactory, virtualThreadsEnabled);
    }

    private void configureAuthentication(BaseSmtpServer smtpServer, FakeSmtpConfigurationProperties.Authentication authentication) {
        if (!StringUtils.hasText(authentication.getUsername())) {
            logger.error("Username is missing; skip configuration of authentication");
        } else if (!StringUtils.hasText(authentication.getPassword())) {
            logger.error("Password is missing; skip configuration of authentication");
        } else {
            logger.info("Setup simple username and password authentication for SMTP server");
            smtpServer.setAuthenticationHandlerFactory(new EasyAuthenticationHandlerFactory(basicUsernamePasswordValidator));
            smtpServer.setRequireAuth(true);
        }
    }

}
