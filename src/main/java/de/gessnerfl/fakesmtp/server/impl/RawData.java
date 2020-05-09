package de.gessnerfl.fakesmtp.server.impl;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

class RawData {
    private final String from;
    private final String to;
    private final byte[] content;
    private MimeMessage mimeMessage;

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

    public MimeMessage toMimeMessage() throws MessagingException {
        if(mimeMessage == null){
            mimeMessage = parseMimeMessage();
        }
        return mimeMessage;
    }

    private MimeMessage parseMimeMessage() throws MessagingException {
        var s = Session.getDefaultInstance(new Properties());
        return new MimeMessage(s, getContentAsStream());
    }

}
