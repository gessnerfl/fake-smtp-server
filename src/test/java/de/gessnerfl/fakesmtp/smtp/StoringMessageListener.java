package de.gessnerfl.fakesmtp.smtp;

import de.gessnerfl.fakesmtp.smtp.server.MessageListener;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class StoringMessageListener implements MessageListener {
    private final static Logger LOGGER = LoggerFactory.getLogger(StoringMessageListener.class);
    private List<StoredMessage> messages = Collections.synchronizedList(new ArrayList<>());

    @Override
    public boolean accept(String from, String recipient) {
        LOGGER.debug("Accepting mail from {} to {}", from, recipient);
        return true;
    }

    @Override
    public void deliver(String from, String recipient, InputStream data) throws IOException {
        LOGGER.debug("Delivering mail from {} to {}", from,  recipient);
        final var bytes = IOUtils.toByteArray(data);
        LOGGER.debug("Creating message from data with " + bytes.length + " bytes");
        this.messages.add(new StoredMessage(from, recipient, bytes));
    }

    public List<StoredMessage> getMessages(){
        return messages;
    }

    public void reset(){
        this.messages.clear();
    }
}
