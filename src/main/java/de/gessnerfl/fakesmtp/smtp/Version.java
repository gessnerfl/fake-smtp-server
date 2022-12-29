package de.gessnerfl.fakesmtp.smtp;

public class Version {

	private Version(){}

	public static String getSpecification() {
		final Package pkg = Version.class.getPackage();
		return pkg == null ? null : pkg.getSpecificationVersion();
	}

	public static String getImplementation() {
		final Package pkg = Version.class.getPackage();
		return pkg == null ? null : pkg.getImplementationVersion();
	}
}
