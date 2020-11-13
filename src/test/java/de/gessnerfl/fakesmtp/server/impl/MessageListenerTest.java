package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageListenerTest {

    @Mock
    private EmailFactory emailFactory;
    @Mock
    private EmailFilter emailFilter;
    @Mock
    private EmailRepository emailRepository;
    @Mock
    private MessageForwarder messageForwarder;
    @Mock
    private Logger logger;

    @InjectMocks
    private MessageListener sut;

    @Test
    void shouldAcceptAllMails(){
        assertTrue(sut.accept("foo", "bar"));
    }

    @Test
    void shouldCreateEmailEntityAndStoreItInDatabaseWhenEmailIsDelivered() throws IOException {
        var from = "from";
        var to = "to";
        var contentString = "content";
        var content = contentString.getBytes(StandardCharsets.UTF_8);
        var contentStream = new ByteArrayInputStream(content);
        var mail = mock(Email.class);

        when(emailFactory.convert(any(RawData.class))).thenReturn(mail);

        sut.deliver(from, to, contentStream);

        ArgumentCaptor<RawData> argumentCaptor = ArgumentCaptor.forClass(RawData.class);
        verify(emailFactory).convert(argumentCaptor.capture());
        RawData rawData = argumentCaptor.getValue();
        assertEquals(from, rawData.getFrom());
        assertEquals(to, rawData.getTo());
        assertEquals(contentString, rawData.getContentAsString());
        verify(emailRepository).save(mail);
        verify(messageForwarder).forward(rawData);
    }

    @Test
    void shouldThrowExceptionWhenEmailEntityCannotBeCreatedWhenEmailIsDelivered() throws IOException {
        var from = "from";
        var to = "to";
        var content = "content".getBytes(StandardCharsets.UTF_8);
        var contentStream = new ByteArrayInputStream(content);

        when(emailFactory.convert(any(RawData.class))).thenThrow(new IOException("foo"));

        assertThrows(IOException.class, () -> {

            sut.deliver(from, to, contentStream);

            verify(emailRepository, never()).save(any(Email.class));
            verify(messageForwarder, never()).forward(any(RawData.class));
        });
    }
}