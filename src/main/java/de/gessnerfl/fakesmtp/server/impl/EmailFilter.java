package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import java.util.Arrays;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailFilter {

  private final FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;
  private final Logger logger;

  @Autowired
  public EmailFilter(FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties, Logger logger) {
    this.fakeSmtpConfigurationProperties = fakeSmtpConfigurationProperties;
    this.logger = logger;
  }

  public boolean ignore(String sender, String recipient){
    if(!StringUtils.hasText(this.fakeSmtpConfigurationProperties.getFilteredEmailRegexList())){
      return false;
    }
    return ignoreParticipant(sender) || ignoreParticipant(recipient);
  }

  private boolean ignoreParticipant(String participant) {
    if(StringUtils.hasText(participant)){
      try{
        if(Arrays.stream(this.fakeSmtpConfigurationProperties.getFilteredEmailRegexList().split(",")).anyMatch(participant::matches)){
          logger.info("Participant '{}' matches a filtered email regex entry. Email will be filtered.", participant);
          return true;
        }
      }catch(RuntimeException e){
        logger.error("Unable to check participant '{}' against configured email filteredEmailRegexList '{}'", participant, this.fakeSmtpConfigurationProperties.getFilteredEmailRegexList(), e);
      }
    }
    return false;
  }
}
