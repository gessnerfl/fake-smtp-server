package de.gessnerfl.fakesmtp.server.smtp.client;

import java.io.IOException;

/**
 * An Authenticator is called by SmartClient after the initial EHLO command and
 * negotiates the authentication of the user for example by issuing the SMTP
 * AUTH command to the server.
 *
 * @see <a href="http://tools.ietf.org/html/rfc4954">RFC 4954: SMTP Service
 *      Extension for Authentication</a>
 */
public interface Authenticator {
	void authenticate() throws IOException;
}
