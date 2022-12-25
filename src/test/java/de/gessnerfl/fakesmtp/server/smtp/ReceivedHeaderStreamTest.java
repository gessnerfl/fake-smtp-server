package de.gessnerfl.fakesmtp.server.smtp;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;

import de.gessnerfl.fakesmtp.server.smtp.io.ReceivedHeaderStream;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class tests a bug in ReceivedHeaderStream which has since been fixed.
 *
 * @see <a href=
 *      "http://www.subethamail.org/se/archive_msg.jsp?msgId=59719">http://www.subethamail.org/se/archive_msg.jsp?msgId=59719</a>
 */
public class ReceivedHeaderStreamTest {
	@SuppressWarnings("unused")
	private final static Logger log = LoggerFactory.getLogger(ReceivedHeaderStreamTest.class);

	@Test
	public void testReceivedHeader() throws Exception {
		final int BUF_SIZE = 10000;
		final int offset = 10;
		final ByteArrayInputStream in = new ByteArrayInputStream("hello world".getBytes());
		try (ReceivedHeaderStream hdrIS
				= new ReceivedHeaderStream(in, "ehlo", InetAddress.getLocalHost(), "foo", null, "123", null)) {
			final byte[] buf = new byte[BUF_SIZE];
			final int len = hdrIS.read(buf, offset, BUF_SIZE - offset);

			final String result = new String(buf, offset, len);

			assertTrue(result.endsWith("\nhello world"));
		}
	}
}
