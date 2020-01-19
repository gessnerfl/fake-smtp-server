package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EmailFilterTest {
  private static final String TEST_EMAIL_ADDRESS_1 = "john@doe.com";
  private static final String TEST_EMAIL_ADDRESS_2 = "jane@doe.com";

  @Mock
  private FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;

  @Mock
  private Logger logger;

  @InjectMocks
  private EmailFilter sut;

  @Test
  public void emptyFilter(){
    when(fakeSmtpConfigurationProperties.getFilteredEmailRegexList()).thenReturn(null);
    assertFalse(sut.ignore(TEST_EMAIL_ADDRESS_1, TEST_EMAIL_ADDRESS_2));

    when(fakeSmtpConfigurationProperties.getFilteredEmailRegexList()).thenReturn("      ");
    assertFalse(sut.ignore(TEST_EMAIL_ADDRESS_1, TEST_EMAIL_ADDRESS_2));
  }

  @Test
  public void noneMatchingFilter(){
    when(fakeSmtpConfigurationProperties.getFilteredEmailRegexList()).thenReturn(".*@google.com");
    assertFalse(sut.ignore(TEST_EMAIL_ADDRESS_1,TEST_EMAIL_ADDRESS_2));
  }

  @Test
  public void matchingFilter(){
    when(fakeSmtpConfigurationProperties.getFilteredEmailRegexList()).thenReturn(".*@doe.com");
    assertTrue(sut.ignore(TEST_EMAIL_ADDRESS_1,TEST_EMAIL_ADDRESS_2));
  }

  @Test
  public void matchingFilterMultipleRegexAnyMatch(){
    when(fakeSmtpConfigurationProperties.getFilteredEmailRegexList()).thenReturn(".*@other\\.com,jane@.*");
    assertTrue(sut.ignore(TEST_EMAIL_ADDRESS_1,TEST_EMAIL_ADDRESS_2));
  }

  @Test
  public void matchingFilterMultipleRegexAllMatch(){
    when(fakeSmtpConfigurationProperties.getFilteredEmailRegexList()).thenReturn(".*@doe\\.com,jane@.*");
    assertTrue(sut.ignore(TEST_EMAIL_ADDRESS_1,TEST_EMAIL_ADDRESS_2));
  }

  @Test
  public void invalidRegex(){
    when(fakeSmtpConfigurationProperties.getFilteredEmailRegexList()).thenReturn("****");
    assertFalse(sut.ignore(TEST_EMAIL_ADDRESS_1,TEST_EMAIL_ADDRESS_2));
  }
}
