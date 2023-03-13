package de.gessnerfl.fakesmtp.smtp.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Removes the dot-stuffing happening during the NNTP and SMTP message transfer
 */
public class DotUnstuffingInputStream extends FilterInputStream {

	private int secondLastByteRead = -1;
	private int lastByteRead = -1;

	public DotUnstuffingInputStream(final InputStream in) {
		super(in);
	}

	/**
	 * Read through the stream, checking for '\r\n.'
	 *
	 * @return the byte read from the stream
	 */
	@Override
	public int read() throws IOException {
		int b = this.in.read();
		if (b == '.' && secondLastByteRead == '\r' && lastByteRead == '\n') {
			// skip this '.' because it should have been stuffed
			b = this.in.read();
		}
		secondLastByteRead = lastByteRead;
		lastByteRead = b;
		return b;
	}

	/**
	 * Read through the stream, checking for '\r\n.'
	 *
	 * @param b   the byte array into which the bytes will be read
	 * @param off the offset into the byte array where the bytes will be inserted
	 * @param len the maximum number of bytes to be read off the stream
	 * @return the number of bytes read
	 */
	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		if (b == null) {
			throw new NullPointerException();
		}
		if (off < 0 || off > b.length || len < 0 || off + len > b.length || off + len < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (len == 0) {
			return 0;
		}

		int c = this.read();
		if (c == -1) {
			return -1;
		}
		b[off] = (byte) c;

		int i = 1;

		for (; i < len; i++) {
			c = this.read();
			if (c == -1) {
				break;
			}
			b[off + i] = (byte) c;
		}

		return i;
	}
}
