package de.gessnerfl.fakesmtp.smtp.command;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

@DirtiesContext
@ActiveProfiles({"integrationtest_with_max_size"})
class MailWithMaxMessageSizeTest extends AbstractCommandIntegrationTest {

	@ParameterizedTest
	@CsvSource({
			"MAIL FROM:<validuser@example.com> SIZE=100, 250 Ok",
			"MAIL FROM:<validuser@example.com>, 250 Ok",
			"MAIL FROM:<validuser@example.com> SIZE=1048577, 552"
	})
	void testSizes(final String command, final String expectedResponse) throws Exception {
		this.expect("220");

		this.send("EHLO foo.com");
		this.expectContains("250-SIZE " + DataSize.of(1, DataUnit.MEGABYTES).toBytes());

		this.send(command);
		this.expect(expectedResponse);
	}
}
