package de.gessnerfl.fakesmtp.model;

public enum ContentType {
    HTML, PLAIN, MULTIPART_ALTERNATIVE, MULTIPART_MIXED, MULTIPART_RELATED, UNDEFINED;

    public static ContentType fromString(String string) {
        if (string.startsWith("multipart/alternative")) return MULTIPART_ALTERNATIVE;
        if (string.startsWith("multipart/mixed")) return MULTIPART_MIXED;
        if (string.startsWith("multipart/related")) return MULTIPART_RELATED;
        if (string.startsWith("text/html")) return HTML;
        if (string.startsWith("text/plain")) return PLAIN;
        return UNDEFINED;
    }
}
