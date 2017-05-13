package de.gessnerfl.fakesmtp.server;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.util.TimestampProvider;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(EmailFactory.class);
    private final static Pattern SUBJECT_PATTERN = Pattern.compile("^Subject: (.*)$");
    public static final String NO_SUBJECT = "<no subject>";

    private final TimestampProvider timestampProvider;

    @Autowired
    public EmailFactory(TimestampProvider timestampProvider) {
        this.timestampProvider = timestampProvider;
    }

    public Email convert(String from, String to, InputStream data) throws IOException {
        String content = convertStreamToString(data);
        String subject = parseSubject(content).orElse(NO_SUBJECT);
        return createEmail(from, to, subject, content);
    }

    private Email createEmail(String from, String to, String subject, String content){
        Email email = new Email();
        email.setFromAddress(from);
        email.setToAddress(to);
        email.setReceivedOn(timestampProvider.now());
        email.setSubject(subject);
        email.setContent(content);
        return email;
    }

    private String convertStreamToString(InputStream data) throws IOException {
        return IOUtils.toString(data, StandardCharsets.UTF_8);
    }

    private Optional<String> parseSubject(String data) {
        try {
            BufferedReader reader = new BufferedReader(new StringReader(data));
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = SUBJECT_PATTERN.matcher(line);
                if (matcher.matches()) {
                    return Optional.of(matcher.group(1));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to parse subject from email", e);
        }
        return Optional.empty();
    }
}
