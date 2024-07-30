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
class EmailRetentionTimerTest {

    @Mock
    private FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;
    @Mock
    private EmailRepository emailRepository;
    @Mock
    private Logger logger;

    @InjectMocks
    private EmailRetentionTimer sut;

    @Test
    void shouldTriggerDeletionWhenDataRetentionIsConfigured(){
        var maxNumber = 5;
        var persistence = mock(FakeSmtpConfigurationProperties.Persistence.class);
        var dataRetention = mock(FakeSmtpConfigurationProperties.DataRetention.class);
        var emailRetention = mock(FakeSmtpConfigurationProperties.DataRetentionSetting.class);
        when(persistence.getDataRetention()).thenReturn(dataRetention);
        when(dataRetention.getEmails()).thenReturn(emailRetention);
        when(emailRetention.isEnabled()).thenReturn(true);
        when(emailRetention.getMaxNumberOfRecords()).thenReturn(maxNumber);
        when(fakeSmtpConfigurationProperties.getPersistence()).thenReturn(persistence);

        sut.deleteOutdatedMails();

        verify(emailRepository).deleteEmailsExceedingDateRetentionLimit(maxNumber);
    }

    @Test
    void shouldNotTriggerDeletionWhenEmailDataRetentionIsInActive(){
        var persistence = mock(FakeSmtpConfigurationProperties.Persistence.class);
        var dataRetention = mock(FakeSmtpConfigurationProperties.DataRetention.class);
        var emailRetention = mock(FakeSmtpConfigurationProperties.DataRetentionSetting.class);
        when(persistence.getDataRetention()).thenReturn(dataRetention);
        when(dataRetention.getEmails()).thenReturn(emailRetention);
        when(emailRetention.isEnabled()).thenReturn(false);
        when(fakeSmtpConfigurationProperties.getPersistence()).thenReturn(persistence);

        sut.deleteOutdatedMails();

        verify(emailRepository, never()).deleteEmailsExceedingDateRetentionLimit(anyInt());
    }

    @Test
    void shouldNotTriggerDeletionWhenConfiguredMaxNumberIsLessOrEqualToZero(){
        var maxNumber = 0;
        var persistence = mock(FakeSmtpConfigurationProperties.Persistence.class);
        var dataRetention = mock(FakeSmtpConfigurationProperties.DataRetention.class);
        var emailRetention = mock(FakeSmtpConfigurationProperties.DataRetentionSetting.class);
        when(persistence.getDataRetention()).thenReturn(dataRetention);
        when(dataRetention.getEmails()).thenReturn(emailRetention);
        when(emailRetention.isEnabled()).thenReturn(true);
        when(emailRetention.getMaxNumberOfRecords()).thenReturn(maxNumber);
        when(fakeSmtpConfigurationProperties.getPersistence()).thenReturn(persistence);

        sut.deleteOutdatedMails();

        verify(emailRepository, never()).deleteEmailsExceedingDateRetentionLimit(anyInt());
    }

}