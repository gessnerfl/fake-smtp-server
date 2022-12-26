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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Adds extra dot if dot occurs in message body at beginning of line (according
 * to RFC1939) Compare also org.apache.james.smtpserver.SMTPInputStream
 */
public class ExtraDotOutputStream extends CRLFOutputStream {
	/**
	 * Constructor that wraps an OutputStream.
	 *
	 * @param out the OutputStream to be wrapped
	 */
	public ExtraDotOutputStream(final OutputStream out) {
		super(out);
	}

	/**
	 * Overrides super writeChunk in order to add a "." if the previous chunk ended
	 * with a new line and a new chunk starts with "."
	 *
	 * @see CRLFOutputStream#writeChunk(byte[], int, int)
	 */
	@Override
	protected void writeChunk(final byte[] buffer, final int offset, final int length) throws IOException {
		if (length > 0 && buffer[offset] == '.' && this.startOfLine) {
			// add extra dot (the first of the pair)
			this.out.write('.');
		}
		super.writeChunk(buffer, offset, length);
	}

	/**
	 * Writes a byte to the stream, adding dots where appropriate. Also fixes any
	 * naked CR or LF to the RFC 2821 mandated CRLF pairing.
	 *
	 * @param b the byte to write
	 *
	 * @throws IOException if an error occurs writing the byte
	 */
	@Override
	public void write(final int b) throws IOException {
		if (b == '.' && this.statusLast != LAST_WAS_OTHER) {
			// add extra dot (the first of the pair)
			this.out.write('.');
		}
		super.write(b);
	}
}
