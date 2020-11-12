package de.gessnerfl.fakesmtp.service;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailRetentionTimerTest {

    @Mock
    private FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;
    @Mock
    private EmailRepository emailRepository;
    @Mock
    private Logger logger;

    @InjectMocks
    private EmailRetentionTimer sut;

    @Test
    public void shouldTriggerDeletionWhenDataRetentionIsConfigured(){
        var maxNumber = 5;
        var persistence = mock(FakeSmtpConfigurationProperties.Persistence.class);
        when(persistence.getMaxNumberEmails()).thenReturn(maxNumber);
        when(fakeSmtpConfigurationProperties.getPersistence()).thenReturn(persistence);

        sut.deleteOutdatedMails();

        verify(emailRepository).deleteEmailsExceedingDateRetentionLimit(maxNumber);
    }

    @Test
    public void shouldNotTriggerDeletionWhenConfiguredMaxNumberIsNull(){
        var persistence = mock(FakeSmtpConfigurationProperties.Persistence.class);
        when(persistence.getMaxNumberEmails()).thenReturn(null);
        when(fakeSmtpConfigurationProperties.getPersistence()).thenReturn(persistence);

        sut.deleteOutdatedMails();

        verify(emailRepository, never()).deleteEmailsExceedingDateRetentionLimit(anyInt());
    }

    @Test
    public void shouldNotTriggerDeletionWhenConfiguredMaxNumberIsLessOrEqualToZero(){
        var persistence = mock(FakeSmtpConfigurationProperties.Persistence.class);
        when(persistence.getMaxNumberEmails()).thenReturn(0);
        when(fakeSmtpConfigurationProperties.getPersistence()).thenReturn(persistence);

        sut.deleteOutdatedMails();

        verify(emailRepository, never()).deleteEmailsExceedingDateRetentionLimit(anyInt());
    }

    @Test
    public void shouldNotTriggerDeletionWhenNoPersistenceIsConfigured(){
        when(fakeSmtpConfigurationProperties.getPersistence()).thenReturn(null);

        sut.deleteOutdatedMails();

        verify(emailRepository, never()).deleteEmailsExceedingDateRetentionLimit(anyInt());
    }

}