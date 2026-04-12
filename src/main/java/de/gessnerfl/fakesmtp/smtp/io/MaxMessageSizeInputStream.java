package de.gessnerfl.fakesmtp.smtp.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MaxMessageSizeInputStream extends FilterInputStream {

    private final long maxMessageSizeInBytes;
    private long bytesRead = 0;

    public MaxMessageSizeInputStream(InputStream in, long maxMessageSizeInBytes) {
        super(in);
        this.maxMessageSizeInBytes = maxMessageSizeInBytes;
    }

    @Override
    public int read() throws IOException {
        int value = super.read();
        if (value != -1) {
            incrementBytesRead(1);
        }
        return value;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int count = super.read(b, off, len);
        if (count > 0) {
            incrementBytesRead(count);
        }
        return count;
    }

    private void incrementBytesRead(int count) throws MaxMessageSizeExceededException {
        bytesRead += count;
        if (bytesRead > maxMessageSizeInBytes) {
            throw new MaxMessageSizeExceededException(maxMessageSizeInBytes);
        }
    }
}
