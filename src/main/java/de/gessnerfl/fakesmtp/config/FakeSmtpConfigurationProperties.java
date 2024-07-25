package de.gessnerfl.fakesmtp.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotNull;
import org.springframework.util.unit.DataSize;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "fakesmtp")
public class FakeSmtpConfigurationProperties {

    private static final int DEFAULT_PORT = 25;

    @NotNull
    private Integer port = DEFAULT_PORT;
    private InetAddress bindAddress;
    private Authentication authentication;
    private List<String> blockedRecipientAddresses = new ArrayList<>();
    private String filteredEmailRegexList;

    private DataSize maxMessageSize;
    private boolean requireTLS = false;
    @Valid
    private KeyStore tlsKeystore;
    private boolean forwardEmails = false;

    @NotNull
    private Persistence persistence = new Persistence();

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public InetAddress getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(InetAddress bindAddress) {
        this.bindAddress = bindAddress;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public Persistence getPersistence() {
        return persistence;
    }

    public void setPersistence(Persistence persistence) {
        this.persistence = persistence;
    }

    public List<String> getBlockedRecipientAddresses() {
        return blockedRecipientAddresses;
    }

    public void setBlockedRecipientAddresses(List<String> blockedRecipientAddresses) {
        this.blockedRecipientAddresses = blockedRecipientAddresses;
    }

    public String getFilteredEmailRegexList() {
        return filteredEmailRegexList;
    }

    public void setFilteredEmailRegexList(String filteredEmailRegexList) {
        this.filteredEmailRegexList = filteredEmailRegexList;
    }

    public DataSize getMaxMessageSize() {
        return maxMessageSize;
    }

    public void setMaxMessageSize(DataSize maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

    public boolean isRequireTLS() {
        return requireTLS;
    }

    public void setRequireTLS(boolean requireTLS) {
        this.requireTLS = requireTLS;
    }

    public KeyStore getTlsKeystore() {
        return tlsKeystore;
    }

    public void setTlsKeystore(KeyStore tlsKeystore) {
        this.tlsKeystore = tlsKeystore;
    }

    public boolean isForwardEmails() {
        return forwardEmails;
    }

    public void setForwardEmails(boolean forwardEmails) {
        this.forwardEmails = forwardEmails;
    }

    public static class Authentication {
        @NotNull
        private String username;
        @NotNull
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Persistence {
        static final int DEFAULT_MAX_NUMBER_EMAILS = 100;
        static final int DEFAULT_FIXED_DELAY = 300000;
        static final int DEFAULT_INITIAL_DELAY = 60000;

        @NotNull
        private Integer maxNumberEmails = DEFAULT_MAX_NUMBER_EMAILS;
        @NotNull
        private Integer fixedDelay = DEFAULT_FIXED_DELAY;
        @NotNull
        private Integer initialDelay = DEFAULT_INITIAL_DELAY;

        public Integer getMaxNumberEmails() {
            return maxNumberEmails;
        }

        public void setMaxNumberEmails(Integer maxNumberEmails) {
            this.maxNumberEmails = maxNumberEmails;
        }

        public Integer getFixedDelay() {
            return fixedDelay;
        }

        public void setFixedDelay(Integer fixedDelay) {
            this.fixedDelay = fixedDelay;
        }

        public Integer getInitialDelay() {
            return initialDelay;
        }

        public void setInitialDelay(Integer initialDelay) {
            this.initialDelay = initialDelay;
        }
    }

    public enum KeyStoreType {
        PKCS12, JKS
    }

    public static class KeyStore {
        @NotEmpty
        private String location;
        @NotEmpty
        private String password;
        @NotNull
        private KeyStoreType type = KeyStoreType.JKS;

        public @NotEmpty String getLocation() {
            return location;
        }

        public void setLocation(@NotEmpty String location) {
            this.location = location;
        }

        public @NotEmpty String getPassword() {
            return password;
        }

        public void setPassword(@NotEmpty String password) {
            this.password = password;
        }

        public @NotNull KeyStoreType getType() {
            return type;
        }

        public void setType(@NotNull KeyStoreType type) {
            this.type = type;
        }
    }
}
