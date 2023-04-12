package de.gessnerfl.fakesmtp.model;

public class ApplicationMetaData {
    private final String version;

    public ApplicationMetaData(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
