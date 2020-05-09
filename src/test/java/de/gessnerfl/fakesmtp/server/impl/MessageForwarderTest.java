package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.springframework.mail.SimpleMailMessage;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MessageForwarderTest {

    @Mock
    private FakeSmtpConfigurationProperties configurationProperties;
    @Mock
    private JavaMailSenderFacade javaMailSenderFacade;
    @Mock
    private Logger logger;

    @InjectMocks
    private MessageForwarder sut;

    @Test
    public void shouldSkipForwardingWhenForwardingIsNotEnabled() throws Exception {
        var rawData = mock(RawData.class);
        when(configurationProperties.isForwardEmails()).thenReturn(false);

        sut.forward(rawData);

        verify(rawData, never()).toMimeMessage();
        verifyNoInteractions(javaMailSenderFacade);
    }

    @Test
    public void shouldForwardMimeMessageWhenForwardingIsEnabledAndEmailCanBeConvertedToMimeMessage() throws Exception {
        var mimeMessage = mock(MimeMessage.class);
        var rawData = mock(RawData.class);
        when(rawData.toMimeMessage()).thenReturn(mimeMessage);
        when(configurationProperties.isForwardEmails()).thenReturn(true);

        sut.forward(rawData);

        verify(rawData).toMimeMessage();
        verify(javaMailSenderFacade).send(mimeMessage);
        verifyNoMoreInteractions(rawData, javaMailSenderFacade);
    }

    @Test
    public void shouldForwardEmailAsSimpleMessageWhenForwardingIsEnabledAndEmailCannotBeConvertedToMimeMessage() throws Exception {
        var expectedException = new MessagingException("test");
        var from = "from";
        var to = "to";
        var content = "content";
        var rawData = mock(RawData.class);
        when(rawData.getFrom()).thenReturn(from);
        when(rawData.getTo()).thenReturn(to);
        when(rawData.getContentAsString()).thenReturn(content);
        when(rawData.toMimeMessage()).thenThrow(expectedException);
        when(configurationProperties.isForwardEmails()).thenReturn(true);

        sut.forward(rawData);

        verify(rawData).toMimeMessage();
        verify(javaMailSenderFacade, never()).send(any(MimeMessage.class));
        verify(logger).warn(anyString(), eq(expectedException));
        verify(rawData).getFrom();
        verify(rawData).getTo();
        verify(rawData).getContentAsString();
        var mailMessageArgumentCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSenderFacade).send(mailMessageArgumentCaptor.capture());
        verifyNoMoreInteractions(rawData, javaMailSenderFacade);

        var message = mailMessageArgumentCaptor.getValue();
        assertEquals(from, message.getFrom());
        assertEquals(to, message.getTo()[0]);
        assertEquals(content, message.getText());
    }

}