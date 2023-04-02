package de.gessnerfl.fakesmtp.smtp.server;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailFilterTest {
  private static final String TEST_EMAIL_ADDRESS_1 = "john@doe.com";
  private static final String TEST_EMAIL_ADDRESS_2 = "jane@doe.com";

  @Mock
  private FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;

  @Mock
  private Logger logger;

  @InjectMocks
  private EmailFilter sut;

  @Test
  void emptyFilter(){
    when(fakeSmtpConfigurationProperties.getFilteredEmailRegexList()).thenReturn(null);
    assertFalse(sut.ignore(TEST_EMAIL_ADDRESS_1, TEST_EMAIL_ADDRESS_2));

    when(fakeSmtpConfigurationProperties.getFilteredEmailRegexList()).thenReturn("      ");
    assertFalse(sut.ignore(TEST_EMAIL_ADDRESS_1, TEST_EMAIL_ADDRESS_2));
  }

  @Test
  void noneMatchingFilter(){
    when(fakeSmtpConfigurationProperties.getFilteredEmailRegexList()).thenReturn(".*@google.com");
    assertFalse(sut.ignore(TEST_EMAIL_ADDRESS_1,TEST_EMAIL_ADDRESS_2));
  }

  @Test
  void matchingFilter(){
    when(fakeSmtpConfigurationProperties.getFilteredEmailRegexList()).thenReturn(".*@doe.com");
    assertTrue(sut.ignore(TEST_EMAIL_ADDRESS_1,TEST_EMAIL_ADDRESS_2));
  }

  @ParameterizedTest
  @ValueSource(strings = {".*@other\\.com,jane@.*", ".*@doe\\.com,jane@.*"})
  void matchingFilterMultipleRegex(String regex){
    when(fakeSmtpConfigurationProperties.getFilteredEmailRegexList()).thenReturn(regex);
    assertTrue(sut.ignore(TEST_EMAIL_ADDRESS_1,TEST_EMAIL_ADDRESS_2));
  }

  @Test
  void invalidRegex(){
    when(fakeSmtpConfigurationProperties.getFilteredEmailRegexList()).thenReturn("****");
    assertFalse(sut.ignore(TEST_EMAIL_ADDRESS_1,TEST_EMAIL_ADDRESS_2));
  }
}
