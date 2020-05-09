package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.TestResourceUtil;
import org.junit.Test;

import javax.mail.internet.MimeMessage;

import static org.junit.Assert.assertEquals;

public class RawDataTest {

    @Test
    public void shouldReturnMimeMessage() throws Exception {
        RawData sut = new RawData("from", "to", TestResourceUtil.getTestFileContentBytes(("mail-with-subject.eml")));

        MimeMessage message = sut.toMimeMessage();
        assertEquals("This is the mail title", message.getSubject());
    }

}