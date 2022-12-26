package de.gessnerfl.fakesmtp.server.smtp;

public class Version {
	public static String getSpecification() {
		final Package pkg = Version.class.getPackage();
		return pkg == null ? null : pkg.getSpecificationVersion();
	}

	public static String getImplementation() {
		final Package pkg = Version.class.getPackage();
		return pkg == null ? null : pkg.getImplementationVersion();
	}
}
