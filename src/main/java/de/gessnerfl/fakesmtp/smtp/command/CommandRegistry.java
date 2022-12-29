package de.gessnerfl.fakesmtp.smtp.command;

import de.gessnerfl.fakesmtp.smtp.server.RequireAuthCommandWrapper;
import de.gessnerfl.fakesmtp.smtp.server.RequireTLSCommandWrapper;

public enum CommandRegistry {
	AUTH(new AuthCommand(), true, false),
	DATA(new DataCommand(), true, true),
	EHLO(new EhloCommand(), false, false),
	HELO(new HelloCommand(), true, false),
	HELP(new HelpCommand(), true, true),
	MAIL(new MailCommand(), true, true),
	NOOP(new NoopCommand(), false, false),
	QUIT(new QuitCommand(), false, false),
	RCPT(new ReceiptCommand(), true, true),
	RSET(new ResetCommand(), true, false),
	STARTTLS(new StartTLSCommand(), false, false),
	VRFY(new VerifyCommand(), true, true),
	EXPN(new ExpandCommand(), true, true);

	private Command command;

	CommandRegistry(final Command cmd,
			final boolean checkForStartedTLSWhenRequired,
			final boolean checkForAuthIfRequired) {
		if (checkForStartedTLSWhenRequired) {
			this.command = new RequireTLSCommandWrapper(cmd);
		} else {
			this.command = cmd;
		}
		if (checkForAuthIfRequired) {
			this.command = new RequireAuthCommandWrapper(this.command);
		}
	}

	public Command getCommand() {
		return this.command;
	}
}
