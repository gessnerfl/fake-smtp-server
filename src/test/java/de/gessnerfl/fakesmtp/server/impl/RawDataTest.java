package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.TestResourceUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.mail.internet.MimeMessage;

class RawDataTest {

    @Test
    void shouldReturnMimeMessage() throws Exception {
        RawData sut = new RawData("from", "to", TestResourceUtil.getTestFileContentBytes(("mail-with-subject.eml")));

        MimeMessage message = sut.toMimeMessage();
        Assertions.assertEquals("This is the mail title", message.getSubject());
    }

}