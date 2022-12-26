/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one * or more
 * contributor license agreements. See the NOTICE file * distributed with this
 * work for additional information * regarding copyright ownership. The ASF
 * licenses this file * to you under the Apache License, Version 2.0 (the *
 * "License"); you may not use this file except in compliance * with the
 * License. You may obtain a copy of the License at * *
 * http://www.apache.org/licenses/LICENSE-2.0 * * Unless required by applicable
 * law or agreed to in writing, * software distributed under the License is
 * distributed on an * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY *
 * KIND, either express or implied. See the License for the * specific language
 * governing permissions and limitations * under the License. *
 ****************************************************************/

package de.gessnerfl.fakesmtp.server.smtp.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A Filter for use with SMTP or other protocols in which lines must end with
 * CRLF. Converts every "isolated" occourency of \r or \n with \r\n
 *
 * RFC 2821 #2.3.7 mandates that line termination is CRLF, and that CR and LF
 * must not be transmitted except in that pairing. If we get a naked LF, convert
 * to CRLF.
 *
 */
public class CRLFOutputStream extends FilterOutputStream {
	/**
	 * Counter for number of last (0A or 0D).
	 */
	protected int statusLast;

	protected  static final int LAST_WAS_OTHER = 0;

	protected static final int LAST_WAS_CR = 1;

	protected static final int LAST_WAS_LF = 2;

	protected boolean startOfLine = true;

	/**
	 * Constructor that wraps an OutputStream.
	 *
	 * @param out the OutputStream to be wrapped
	 */
	public CRLFOutputStream(final OutputStream out) {
		super(out);
		this.statusLast = LAST_WAS_LF; // we already assume a CRLF at beginning
										// (otherwise TOP would not work correctly
										// !)
	}

	/**
	 * Writes a byte to the stream Fixes any naked CR or LF to the RFC 2821 mandated
	 * CFLF pairing.
	 *
	 * @param b the byte to write
	 *
	 * @throws IOException if an error occurs writing the byte
	 */
	@Override
	public void write(final int b) throws IOException {
		switch (b) {
		case '\r':
			this.out.write('\r');
			this.out.write('\n');
			this.startOfLine = true;
			this.statusLast = LAST_WAS_CR;
			break;
		case '\n':
			if (this.statusLast != LAST_WAS_CR) {
				this.out.write('\r');
				this.out.write('\n');
				this.startOfLine = true;
			}
			this.statusLast = LAST_WAS_LF;
			break;
		default:
			// we're no longer at the start of a line
			this.out.write(b);
			this.startOfLine = false;
			this.statusLast = LAST_WAS_OTHER;
			break;
		}
	}

	/**
	 * Provides an extension point for ExtraDotOutputStream to be able to add dots
	 * at the beginning of new lines.
	 *
	 * @see java.io.FilterOutputStream#write(byte[], int, int)
	 */
	protected void writeChunk(final byte[] buffer, final int offset, final int length) throws IOException {
		this.out.write(buffer, offset, length);
	}

	/**
	 * @see java.io.FilterOutputStream#write(byte[], int, int)
	 */
	@Override
	public synchronized void write(final byte[] buffer, final int offset, final int length) throws IOException {
		/* optimized */
		int lineStart = offset;
		for (int i = offset; i < length + offset; i++) {
			switch (buffer[i]) {
			case '\r':
				// CR case. Write down the last line
				// and position the new lineStart at the next char
				this.writeChunk(buffer, lineStart, i - lineStart);
				this.out.write('\r');
				this.out.write('\n');
				this.startOfLine = true;
				lineStart = i + 1;
				this.statusLast = LAST_WAS_CR;
				break;
			case '\n':
				if (this.statusLast != LAST_WAS_CR) {
					this.writeChunk(buffer, lineStart, i - lineStart);
					this.out.write('\r');
					this.out.write('\n');
					this.startOfLine = true;
				}
				lineStart = i + 1;
				this.statusLast = LAST_WAS_LF;
				break;
			default:
				this.statusLast = LAST_WAS_OTHER;
			}
		}
		if (length + offset > lineStart) {
			this.writeChunk(buffer, lineStart, length + offset - lineStart);
			this.startOfLine = false;
		}
	}

	/**
	 * Ensure that the stream is CRLF terminated.
	 *
	 * @throws IOException if an error occurs writing the byte
	 */
	public void checkCRLFTerminator() throws IOException {
		if (this.statusLast == LAST_WAS_OTHER) {
			this.out.write('\r');
			this.out.write('\n');
			this.statusLast = LAST_WAS_CR;
		}
	}
}
