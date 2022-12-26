package de.gessnerfl.fakesmtp.server.smtp.client;

import java.io.IOException;

/**
 * Thrown if a syntactically valid reply was received from the server, which
 * indicates an error via the status code.
 */
@SuppressWarnings("serial")
public class SMTPException extends IOException {
	private final transient SMTPClient.Response response;

	public SMTPException(final SMTPClient.Response resp) {
		super(resp.toString());

		this.response = resp;
	}

	public SMTPClient.Response getResponse() {
		return this.response;
	}
}
