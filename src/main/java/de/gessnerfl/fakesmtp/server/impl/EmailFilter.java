package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import java.util.Arrays;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailFilter {

  private final String filteredEmailRegexList;
  private final Logger logger;

  public EmailFilter(FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties, Logger logger) {
    filteredEmailRegexList = fakeSmtpConfigurationProperties.getFilteredEmailRegexList();
    this.logger = logger;
  }

  public boolean ignore(String sender, String recipient){
    if(StringUtils.isEmpty(filteredEmailRegexList)){
      return false;
    }
    return ignoreParticipant(sender) || ignoreParticipant(recipient);
  }

  private boolean ignoreParticipant(String participant) {
    if(StringUtils.hasText(participant)){
      try{
        if(Arrays.stream(filteredEmailRegexList.split(",")).anyMatch(emailRegex -> participant.matches(emailRegex))){
          logger.info("Participant '{}' matches a filtered email regex entry. Email will be filtered.", participant);
          return true;
        }
      }catch(RuntimeException e){
        logger.error("Unable to check participant '{}' against configured email filteredEmailRegexList '{}'", participant, filteredEmailRegexList, e);
      }
    }
    return false;
  }
}
