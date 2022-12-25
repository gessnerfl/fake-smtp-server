/***********************************************************************
 * Copyright (c) 2000-2006 The Apache Software Foundation. * All rights
 * reserved. *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you * may not
 * use this file except in compliance with the License. You * may obtain a copy
 * of the License at: * * http://www.apache.org/licenses/LICENSE-2.0 * * Unless
 * required by applicable law or agreed to in writing, software * distributed
 * under the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or * implied. See the License for the
 * specific language governing * permissions and limitations under the License.
 * *
 ***********************************************************************/

package de.gessnerfl.fakesmtp.server.smtp.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Removes the dot-stuffing happening during the NNTP and SMTP message transfer
 */
public class DotUnstuffingInputStream extends FilterInputStream {
	/**
	 * An array to hold the last two bytes read off the stream. This allows the
	 * stream to detect '\r\n' sequences even when they occur across read
	 * boundaries.
	 */
	protected int[] last = { -1, -1 };

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
		if (b == '.' && this.last[0] == '\r' && this.last[1] == '\n') {
			// skip this '.' because it should have been stuffed
			b = this.in.read();
		}
		this.last[0] = this.last[1];
		this.last[1] = b;
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

	/**
	 * Provide access to the base input stream.
	 */
	public InputStream getBaseStream() {
		return this.in;
	}
}
