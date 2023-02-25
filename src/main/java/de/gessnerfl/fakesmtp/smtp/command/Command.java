package de.gessnerfl.fakesmtp.smtp.command;

import java.io.IOException;

import de.gessnerfl.fakesmtp.smtp.server.HelpMessage;
import de.gessnerfl.fakesmtp.smtp.server.Session;

public interface Command {
	void execute(String commandString, Session sess) throws IOException;

	HelpMessage getHelp() throws CommandException;

	/**
	 * Returns the name of the command in upper case. For example "QUIT".
	 */
	CommandVerb getVerb();
}
