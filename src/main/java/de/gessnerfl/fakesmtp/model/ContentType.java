package de.gessnerfl.fakesmtp.model;

import java.util.Locale;

public enum ContentType {
    HTML, PLAIN, MULTIPART_ALTERNATIVE, MULTIPART_MIXED, MULTIPART_RELATED, UNDEFINED, IMAGE, OCTET_STREAM;

    public static ContentType fromString(String string) {
        string = string.toLowerCase(Locale.ENGLISH);
        if (string.startsWith("multipart/alternative")) return MULTIPART_ALTERNATIVE;
        if (string.startsWith("multipart/mixed")) return MULTIPART_MIXED;
        if (string.startsWith("multipart/related")) return MULTIPART_RELATED;
        if (string.startsWith("text/html")) return HTML;
        if (string.startsWith("text/plain")) return PLAIN;
        if (string.startsWith("image/")) return IMAGE;
        if (string.startsWith("application/octet-stream")) return OCTET_STREAM;
        return UNDEFINED;
    }
}
