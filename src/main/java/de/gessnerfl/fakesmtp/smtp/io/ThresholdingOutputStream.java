package de.gessnerfl.fakesmtp.smtp.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This is an OutputStream wrapper which takes notice when a threshold (number
 * of bytes) is about to be written. This can be used to limit output data, swap
 * writers, etc.
 */
public abstract class ThresholdingOutputStream extends OutputStream {
	protected OutputStream output;

	/** When to trigger */
	private final int threshold;

	/** Number of bytes written so far */
	private int written = 0;

	private boolean thresholdReached = false;

	/**
	 */
	protected ThresholdingOutputStream(final OutputStream base, final int thresholdBytes) {
		this.output = base;
		this.threshold = thresholdBytes;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.io.OutputStream#close()
	 */
	@Override
	public void close() throws IOException {
		this.output.close();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.io.OutputStream#flush()
	 */
	@Override
	public void flush() throws IOException {
		this.output.flush();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		this.checkThreshold(len);

		this.output.write(b, off, len);

		this.written += len;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.io.OutputStream#write(byte[])
	 */
	@Override
	public void write(final byte[] b) throws IOException {
		this.checkThreshold(b.length);

		this.output.write(b);

		this.written += b.length;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(final int b) throws IOException {
		this.checkThreshold(1);

		this.output.write(b);

		this.written++;
	}

	/**
	 * Checks whether reading count bytes would cross the limit.
	 */
	protected void checkThreshold(final int count) throws IOException {
		final int predicted = this.written + count;
		if (!this.thresholdReached && predicted > this.threshold) {
			this.thresholdReached(this.written, predicted);
			this.thresholdReached = true;
		}
	}

	/**
	 * Called when the threshold is about to be exceeded. This isn't exact; it's
	 * called whenever a write would occur that would cross the amount. Once it is
	 * called, it isn't called again.
	 *
	 * @param current   is the current number of bytes that have been written
	 * @param predicted is the total number after the write completes
	 */
	protected abstract void thresholdReached(int current, int predicted) throws IOException;
}
