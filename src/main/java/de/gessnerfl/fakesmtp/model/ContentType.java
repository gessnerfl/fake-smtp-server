package de.gessnerfl.fakesmtp.model;

public enum ContentType {
    HTML, PLAIN, MULTIPART_ALTERNATIVE;

    public static ContentType fromString(String string) {
        if (string.startsWith("multipart/alternative")) return MULTIPART_ALTERNATIVE;
        if (string.startsWith("text/html")) return HTML;
        return PLAIN;
    }
}
