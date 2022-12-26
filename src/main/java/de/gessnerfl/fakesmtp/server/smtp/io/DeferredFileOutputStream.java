package de.gessnerfl.fakesmtp.server.smtp.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * This works like a ByteArrayOutputStream until a certain size is reached, then
 * creates a temp file and acts like a buffered FileOutputStream. The data can
 * be retreived afterwards by calling getInputStream().
 *
 * When this object is closed, the temporary file is deleted. You can no longer
 * call getInputStream().
 */
public class DeferredFileOutputStream extends ThresholdingOutputStream {
	/**
	 * Initial size of the byte array buffer. Better to make this large to start
	 * with so that we can avoid reallocs; mail messages are rarely tiny.
	 */
	static final int INITIAL_BUF_SIZE = 8192;

	public static final String TMPFILE_PREFIX = "subetha";

	public static final String TMPFILE_SUFFIX = ".msg";

	/** If we switch to file output, this is the file. */
	File outFile;

	/** If we switch to file output, this is the stream. */
	FileOutputStream outFileStream;

	/** When the output stream is closed, this becomes true */
	boolean closed;

	boolean thresholdReached = false;

	/**
	 * @param transitionSize is the number of bytes at which to convert from a byte
	 *                       array to a real file.
	 */
	public DeferredFileOutputStream(final int transitionSize) {
		super(new BetterByteArrayOutputStream(INITIAL_BUF_SIZE), transitionSize);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.subethamail.common.io.ThresholdingOutputStream#thresholdReached(int,
	 * int)
	 */
	@Override
	protected void thresholdReached(final int current, final int predicted) throws IOException {
		// Open a temp file, write the byte array version, and swap the
		// output stream to the file version.

		var attr = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
		var outFile = Files.createTempFile(TMPFILE_PREFIX, TMPFILE_SUFFIX, attr);
		this.outFile = outFile.toFile();
		this.outFileStream = new FileOutputStream(this.outFile);

		((ByteArrayOutputStream) this.output).writeTo(this.outFileStream);
		this.output = new BufferedOutputStream(this.outFileStream);
	}

	/**
	 * Closes the output stream and creates an InputStream on the same data.
	 *
	 * @return either a BetterByteArrayOutputStream or buffered FileInputStream,
	 *         depending on what state we are in.
	 */
	public InputStream getInputStream() throws IOException {
		if (this.output instanceof BetterByteArrayOutputStream) {
			return ((BetterByteArrayOutputStream) this.output).getInputStream();
		}
		if (!this.closed) {
			this.output.flush();
			this.output.close();
			this.closed = true;
		}

		return new BufferedInputStream(new FileInputStream(this.outFile));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.subethamail.common.io.ThresholdingOutputStream#close()
	 */
	@Override
	public void close() throws IOException {
		if (!this.closed) {
			this.output.flush();
			this.output.close();
			this.closed = true;
		}

		if (this.outFile != null) {
			this.outFile.delete();
		}
	}
}
