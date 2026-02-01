package de.gessnerfl.fakesmtp.model;

public class ApplicationMetaData {
    private final String version;
    private final boolean authenticationEnabled;
    private final boolean authenticated;
    private final int sessionTimeoutMinutes;

    public ApplicationMetaData(String version, boolean authenticationEnabled, boolean authenticated, int sessionTimeoutMinutes) {
        this.version = version;
        this.authenticationEnabled = authenticationEnabled;
        this.authenticated = authenticated;
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    public String getVersion() {
        return version;
    }

    public boolean isAuthenticationEnabled() {
        return authenticationEnabled;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public int getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }
}
