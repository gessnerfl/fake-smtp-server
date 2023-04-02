package de.gessnerfl.fakesmtp.smtp.server;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlockedRecipientAddresses {

    final List<String> blockedAddresses;

    @Autowired
    public BlockedRecipientAddresses(FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties) {
        this.blockedAddresses = fakeSmtpConfigurationProperties.getBlockedRecipientAddresses()
                .stream()
                .map(String::toLowerCase)
                .toList();
    }

    public boolean isBlocked(String recipient){
        return recipient != null && blockedAddresses.contains(recipient.toLowerCase());
    }
}
