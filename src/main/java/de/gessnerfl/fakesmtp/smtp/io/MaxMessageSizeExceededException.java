package de.gessnerfl.fakesmtp.smtp.io;

import java.io.IOException;

public class MaxMessageSizeExceededException extends IOException {

    public MaxMessageSizeExceededException(long maxMessageSizeInBytes) {
        super("Maximum message size of " + maxMessageSizeInBytes + " bytes exceeded");
    }
}
