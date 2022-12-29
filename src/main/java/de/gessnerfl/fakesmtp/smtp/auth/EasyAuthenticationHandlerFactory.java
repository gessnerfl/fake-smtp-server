package de.gessnerfl.fakesmtp.smtp.auth;

/**
 * This a convenient class that saves you setting up the factories that we know
 * about; you can always add more afterwards. Currently this factory supports:
 *
 * PLAIN LOGIN
 */
public class EasyAuthenticationHandlerFactory extends MultipleAuthenticationHandlerFactory {
	/** Just hold on to this so that the caller can get it later, if necessary */
	UsernamePasswordValidator validator;

	public EasyAuthenticationHandlerFactory(final UsernamePasswordValidator validator) {
		this.validator = validator;

		this.addFactory(new PlainAuthenticationHandlerFactory(this.validator));
		this.addFactory(new LoginAuthenticationHandlerFactory(this.validator));
	}

	public UsernamePasswordValidator getValidator() {
		return this.validator;
	}
}
