/*
 * Commands.java Created on November 18, 2006, 12:26 PM To change this template,
 * choose Tools | Template Manager and open the template in the editor.
 */

package de.gessnerfl.fakesmtp.server.smtp.server;

import de.gessnerfl.fakesmtp.server.smtp.command.AuthCommand;
import de.gessnerfl.fakesmtp.server.smtp.command.DataCommand;
import de.gessnerfl.fakesmtp.server.smtp.command.EhloCommand;
import de.gessnerfl.fakesmtp.server.smtp.command.ExpandCommand;
import de.gessnerfl.fakesmtp.server.smtp.command.HelloCommand;
import de.gessnerfl.fakesmtp.server.smtp.command.HelpCommand;
import de.gessnerfl.fakesmtp.server.smtp.command.MailCommand;
import de.gessnerfl.fakesmtp.server.smtp.command.NoopCommand;
import de.gessnerfl.fakesmtp.server.smtp.command.QuitCommand;
import de.gessnerfl.fakesmtp.server.smtp.command.ReceiptCommand;
import de.gessnerfl.fakesmtp.server.smtp.command.ResetCommand;
import de.gessnerfl.fakesmtp.server.smtp.command.StartTLSCommand;
import de.gessnerfl.fakesmtp.server.smtp.command.VerifyCommand;

/**
 * Enumerates all the Commands made available in this release.
 *
 * @author Marco Trevisan &lt;mrctrevisan@yahoo.it&gt;
 */
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
