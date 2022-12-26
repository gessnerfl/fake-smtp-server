package de.gessnerfl.fakesmtp.server.smtp.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlainAuthenticatorTest {
	@Mock
	private SmartClient smartClient;

	private final Map<String, String> extensions = new HashMap<>();

	// @Test
	void testSuccess() throws IOException {
		extensions.put("AUTH", "GSSAPI DIGEST-MD5 PLAIN");
		final PlainAuthenticator authenticator = new PlainAuthenticator(smartClient, "test", "1234");

		when(smartClient.getExtensions()).thenReturn(extensions);

		authenticator.authenticate();

		verify(smartClient).getExtensions();
		verify(smartClient).sendAndCheck("AUTH PLAIN AHRlc3QAMTIzNA==");
	}
}
