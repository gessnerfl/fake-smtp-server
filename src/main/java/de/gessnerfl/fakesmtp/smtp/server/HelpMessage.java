package de.gessnerfl.fakesmtp.smtp.server;

import de.gessnerfl.fakesmtp.smtp.command.CommandVerb;

import java.util.Objects;
import java.util.StringTokenizer;

public class HelpMessage {
	private final CommandVerb commandVerb;

	private final String argumentDescription;

	private final String helpMessageText;

	private String outputString;

	public HelpMessage(final CommandVerb commandVerb, final String helpMessageText, final String argumentDescription) {
		this.commandVerb = commandVerb;
		this.argumentDescription = argumentDescription == null ? "" : " " + argumentDescription;
		this.helpMessageText = helpMessageText;
		this.buildOutputString();
	}

	public HelpMessage(final CommandVerb commandVerb, final String helpMessageText) {
		this(commandVerb, helpMessageText, null);
	}

	public CommandVerb getName() {
		return this.commandVerb;
	}

	public String toOutputString() {
		return this.outputString;
	}

	private void buildOutputString() {
		final StringTokenizer stringTokenizer = new StringTokenizer(this.helpMessageText, "\n");
		final StringBuilder stringBuilder
				= new StringBuilder().append("214-").append(this.commandVerb).append(this.argumentDescription);
		while (stringTokenizer.hasMoreTokens()) {
			stringBuilder.append("\n214-    ").append(stringTokenizer.nextToken());
		}

		stringBuilder.append("\n214 End of ").append(this.commandVerb).append(" info");
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
				&& (Objects.equals(this.commandVerb, that.commandVerb))
				&& (Objects.equals(this.helpMessageText, that.helpMessageText));
	}

	@Override
	public int hashCode() {
		int result;
		result = this.commandVerb != null ? this.commandVerb.hashCode() : 0;
		result = 29 * result + (this.argumentDescription != null ? this.argumentDescription.hashCode() : 0);
		result = 29 * result + (this.helpMessageText != null ? this.helpMessageText.hashCode() : 0);
		return result;
	}
}
