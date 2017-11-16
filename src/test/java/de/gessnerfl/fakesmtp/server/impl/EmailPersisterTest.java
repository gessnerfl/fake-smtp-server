package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
        final String from = "from";
        final String to = "to";
        final byte[] content = "content".getBytes(StandardCharsets.UTF_8);
        final InputStream contentStream = new ByteArrayInputStream(content);
        final Email mail = mock(Email.class);

        when(emailFactory.convert(from, to, contentStream)).thenReturn(mail);

        sut.deliver(from, to, contentStream);

        verify(emailRepository).save(mail);
    }

    @Test(expected = IOException.class)
    public void shouldThrowExceptionWhenEmailEntityCannotBeCreatedWhenEmailIsDelivered() throws IOException {
        final String from = "from";
        final String to = "to";
        final byte[] content = "content".getBytes(StandardCharsets.UTF_8);
        final InputStream contentStream = new ByteArrayInputStream(content);

        when(emailFactory.convert(from, to, contentStream)).thenThrow(new IOException("foo"));

        sut.deliver(from, to, contentStream);

        verify(emailRepository, never()).save(any(Email.class));
    }
}