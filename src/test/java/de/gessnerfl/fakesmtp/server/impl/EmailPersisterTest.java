package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EmailPersisterTest {

    @Mock
    private EmailFactory emailFactory;
    @Mock
    private EmailRepository emailRepository;
    @Mock
    private Logger logger;

    @InjectMocks
    private EmailPersister sut;

    @Test
    public void shouldAcceptAllMails(){
        assertTrue(sut.accept("foo", "bar"));
    }

    @Test
    public void shouldCreateEmailEntityAndStoreItInDatabaseWhenEmailIsDelivered() throws IOException {
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
    }

    @Test(expected = IOException.class)
    public void shouldThrowExceptionWhenEmailEntityCannotBeCreatedWhenEmailIsDelivered() throws IOException {
        var from = "from";
        var to = "to";
        var content = "content".getBytes(StandardCharsets.UTF_8);
        var contentStream = new ByteArrayInputStream(content);

        when(emailFactory.convert(any(RawData.class))).thenThrow(new IOException("foo"));

        sut.deliver(from, to, contentStream);

        verify(emailRepository, never()).save(any(Email.class));
    }
}