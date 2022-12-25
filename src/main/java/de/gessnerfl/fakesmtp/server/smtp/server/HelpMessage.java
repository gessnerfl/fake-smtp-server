package de.gessnerfl.fakesmtp.server.smtp.server;

import java.util.StringTokenizer;

/**
 * @author Ian McFarland &lt;ian@neo.com&gt;
 * @author Jon Stevens
 */
public class HelpMessage {
	private final String commandName;

	private final String argumentDescription;

	private final String helpMessage;

	private String outputString;

	public HelpMessage(final String commandName, final String helpMessage, final String argumentDescription) {
		this.commandName = commandName;
		this.argumentDescription = argumentDescription == null ? "" : " " + argumentDescription;
		this.helpMessage = helpMessage;
		this.buildOutputString();
	}

	public HelpMessage(final String commandName, final String helpMessage) {
		this(commandName, helpMessage, null);
	}

	public String getName() {
		return this.commandName;
	}

	public String toOutputString() {
		return this.outputString;
	}

	private void buildOutputString() {
		final StringTokenizer stringTokenizer = new StringTokenizer(this.helpMessage, "\n");
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
		if ((this.argumentDescription != null
				? !this.argumentDescription.equals(that.argumentDescription)
				: that.argumentDescription != null)
				|| (this.commandName != null ? !this.commandName.equals(that.commandName) : that.commandName != null)
				|| (this.helpMessage != null ? !this.helpMessage.equals(that.helpMessage) : that.helpMessage != null)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result;
		result = this.commandName != null ? this.commandName.hashCode() : 0;
		result = 29 * result + (this.argumentDescription != null ? this.argumentDescription.hashCode() : 0);
		result = 29 * result + (this.helpMessage != null ? this.helpMessage.hashCode() : 0);
		return result;
	}
}
