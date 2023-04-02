package de.gessnerfl.fakesmtp.smtp.command;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;


@DirtiesContext
@ActiveProfiles({"integrationtest_with_max_size"})
class HelloWithMaxMessageSizeTest extends AbstractCommandIntegrationTest {

	@Test
	void testEhloSize() throws Exception {
		this.expect("220");

		this.send("EHLO foo.com");
		this.expectContains("250-SIZE " + DataSize.of(1, DataUnit.MEGABYTES).toBytes());
	}
}
