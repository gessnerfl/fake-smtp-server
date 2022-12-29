package de.gessnerfl.fakesmtp.smtp.auth;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BasicUsernamePasswordValidator implements UsernamePasswordValidator {

    private final FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;

    @Autowired
    public BasicUsernamePasswordValidator(FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties) {
        this.fakeSmtpConfigurationProperties = fakeSmtpConfigurationProperties;
    }

    @Override
    public void login(String username, String password) throws LoginFailedException {
        if(!isUsernameValid(username) || !isPasswordValid(password)){
            throw new LoginFailedException("Invalid Username or Password");
        }
    }

    private boolean isUsernameValid(String username) {
        return getAuthentication().getUsername().equals(username);
    }

    private boolean isPasswordValid(String password) {
        return getAuthentication().getPassword().equals(password);
    }

    private FakeSmtpConfigurationProperties.Authentication getAuthentication() {
        return fakeSmtpConfigurationProperties.getAuthentication();
    }
}
