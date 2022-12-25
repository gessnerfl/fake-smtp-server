/*
 * $Id$ $URL$
 */
package de.gessnerfl.fakesmtp.server.smtp;

/**
 * Provides version information from the manifest.
 *
 * @author Jeff Schnitzer
 */
public class Version {
	public static String getSpecification() {
		final Package pkg = Version.class.getPackage();
		return pkg == null ? null : pkg.getSpecificationVersion();
	}

	public static String getImplementation() {
		final Package pkg = Version.class.getPackage();
		return pkg == null ? null : pkg.getImplementationVersion();
	}

	/**
	 * A simple main method that prints the version and exits
	 */
	public static void main(final String[] args) {
		System.out.println("Version: " + getSpecification());
		System.out.println("Implementation: " + getImplementation());
	}
}
