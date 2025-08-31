package de.gessnerfl.fakesmtp.model;

public class ApplicationMetaData {
    private final String version;
    private final boolean authenticationEnabled;

    public ApplicationMetaData(String version, boolean authenticationEnabled) {
        this.version = version;
        this.authenticationEnabled = authenticationEnabled;
    }

    public String getVersion() {
        return version;
    }

    public boolean isAuthenticationEnabled() {
        return authenticationEnabled;
    }
}
