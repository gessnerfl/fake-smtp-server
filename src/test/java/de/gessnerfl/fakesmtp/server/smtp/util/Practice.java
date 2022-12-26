package de.gessnerfl.fakesmtp.server.smtp.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import de.gessnerfl.fakesmtp.server.smtp.Wiser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple command-line tool that lets us practice with the smtp library.
 */
public class Practice {
	@SuppressWarnings("unused")
	private final static Logger log = LoggerFactory.getLogger(Practice.class);

	public static final int PORT = 2566;

	public static void main(final String[] args) throws Exception {
		final Wiser wiser = new Wiser();
		wiser.setHostname("localhost");
		wiser.setPort(PORT);

		wiser.start();

		String line;
		final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		do {
			line = in.readLine();
			line = line.trim();

			if ("dump".equals(line)) {
				wiser.dumpMessages(System.out);
			} else if (line.startsWith("dump ")) {
				line = line.substring("dump ".length());
				final File f = new File(line);
				final OutputStream out = new FileOutputStream(f);
				wiser.dumpMessages(new PrintStream(out));
				out.close();
			}
		} while (!"quit".equals(line));

		wiser.stop();
	}
}
