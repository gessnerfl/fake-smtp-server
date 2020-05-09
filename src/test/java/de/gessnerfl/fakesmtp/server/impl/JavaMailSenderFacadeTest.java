package de.gessnerfl.fakesmtp.server.impl;

import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import javax.mail.internet.MimeMessage;

import static org.mockito.Mockito.*;

public class JavaMailSenderFacadeTest {

    @Test
    public void shouldSendMimeMessageWhenMailSystemIsAvailable() {
        var javaMailSender = mock(JavaMailSender.class);
        var logger = mock(Logger.class);
        var mimeMessage = mock(MimeMessage.class);

        var sut = new JavaMailSenderFacade();
        sut.setJavaMailSender(javaMailSender);
        sut.setLogger(logger);

        sut.send(mimeMessage);

        verify(javaMailSender).send(mimeMessage);
        verifyNoMoreInteractions(javaMailSender);
        verifyNoInteractions(logger);
    }

    @Test
    public void shouldLogErrorAndSkipSendingOfMimeMessageWhenMailSystemIsNotAvailable() {
        var logger = mock(Logger.class);
        var mimeMessage = mock(MimeMessage.class);

        var sut = new JavaMailSenderFacade();
        sut.setLogger(logger);

        sut.send(mimeMessage);

        verify(logger).error(JavaMailSenderFacade.ERROR_MESSAGE);
        verifyNoMoreInteractions(logger);
    }

    @Test
    public void shouldSendSimpleMessageWhenMailSystemIsAvailable() {
        var javaMailSender = mock(JavaMailSender.class);
        var logger = mock(Logger.class);
        var message = mock(SimpleMailMessage.class);

        var sut = new JavaMailSenderFacade();
        sut.setJavaMailSender(javaMailSender);
        sut.setLogger(logger);

        sut.send(message);

        verify(javaMailSender).send(message);
        verifyNoMoreInteractions(javaMailSender);
        verifyNoInteractions(logger);
    }

    @Test
    public void shouldLogErrorAndSkipSendingOfSimpleMessageWhenMailSystemIsNotAvailable() {
        var logger = mock(Logger.class);
        var message = mock(SimpleMailMessage.class);

        var sut = new JavaMailSenderFacade();
        sut.setLogger(logger);

        sut.send(message);

        verify(logger).error(JavaMailSenderFacade.ERROR_MESSAGE);
        verifyNoMoreInteractions(logger);
    }

}