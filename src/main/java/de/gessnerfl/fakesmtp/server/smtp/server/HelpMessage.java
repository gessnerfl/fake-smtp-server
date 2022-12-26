package de.gessnerfl.fakesmtp.server.smtp.server;

import java.util.Objects;
import java.util.StringTokenizer;

public class HelpMessage {
	private final String commandName;

	private final String argumentDescription;

	private final String helpMessageText;

	private String outputString;

	public HelpMessage(final String commandName, final String helpMessageText, final String argumentDescription) {
		this.commandName = commandName;
		this.argumentDescription = argumentDescription == null ? "" : " " + argumentDescription;
		this.helpMessageText = helpMessageText;
		this.buildOutputString();
	}

	public HelpMessage(final String commandName, final String helpMessageText) {
		this(commandName, helpMessageText, null);
	}

	public String getName() {
		return this.commandName;
	}

	public String toOutputString() {
		return this.outputString;
	}

	private void buildOutputString() {
		final StringTokenizer stringTokenizer = new StringTokenizer(this.helpMessageText, "\n");
		final StringBuilder stringBuilder
				= new StringBuilder().append("214-").append(this.commandName).append(this.argumentDescription);
		while (stringTokenizer.hasMoreTokens()) {
			stringBuilder.append("\n214-    ").append(stringTokenizer.nextToken());
		}

		stringBuilder.append("\n214 End of ").append(this.commandName).append(" info");
		this.outputString = stringBuilder.toString();
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || this.getClass() != o.getClass()) {
			return false;
		}
		final HelpMessage that = (HelpMessage) o;
		return (Objects.equals(this.argumentDescription, that.argumentDescription))
				&& (Objects.equals(this.commandName, that.commandName))
				&& (Objects.equals(this.helpMessageText, that.helpMessageText));
	}

	@Override
	public int hashCode() {
		int result;
		result = this.commandName != null ? this.commandName.hashCode() : 0;
		result = 29 * result + (this.argumentDescription != null ? this.argumentDescription.hashCode() : 0);
		result = 29 * result + (this.helpMessageText != null ? this.helpMessageText.hashCode() : 0);
		return result;
	}
}
