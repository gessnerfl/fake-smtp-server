package de.gessnerfl.fakesmtp.server.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class RawData {
    private final String from;
    private final String to;
    private final byte[] content;

    RawData(String from, String to, byte[] content) {
        this.from = from;
        this.to = to;
        this.content = content;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getContentAsString() {
        return new String(content, StandardCharsets.UTF_8);
    }

    public InputStream getContentAsStream() {
        return new ByteArrayInputStream(content);
    }

}
