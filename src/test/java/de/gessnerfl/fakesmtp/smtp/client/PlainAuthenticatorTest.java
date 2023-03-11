package de.gessnerfl.fakesmtp.smtp.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlainAuthenticatorTest {
	@Mock
	private SmartClient smartClient;

	private final Map<String, String> extensions = new HashMap<>();

	@Test
	void testSuccess() throws IOException {
		extensions.put("AUTH", "GSSAPI DIGEST-MD5 PLAIN");
		final PlainAuthenticator authenticator = new PlainAuthenticator(smartClient, "test", "1234");

		when(smartClient.getExtensions()).thenReturn(extensions);

		authenticator.authenticate();

		verify(smartClient).getExtensions();
		verify(smartClient).sendAndCheck("AUTH PLAIN AHRlc3QAMTIzNA==");
	}
}
