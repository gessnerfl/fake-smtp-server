package de.gessnerfl.fakesmtp.smtp.command;

import de.gessnerfl.fakesmtp.config.SmtpCommandConfig;
import de.gessnerfl.fakesmtp.smtp.server.InvalidCommandNameException;
import de.gessnerfl.fakesmtp.smtp.server.UnknownCommandException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CommandRegistryTest {

    private CommandRegistry sut;

    @BeforeEach
    void init(){
        sut =  new SmtpCommandConfig().commandRegistry();
    }

    @ParameterizedTest
    @ValueSource(strings = {"help", "HELP", "hElP"})
    void shouldReturnCommandByKeyCaseInsensitive(final String commandName) throws InvalidCommandNameException, UnknownCommandException {
        final var c = sut.getCommandFromString(commandName);

        assertNotNull(c);
        assertEquals(CommandVerb.HELP, c.getVerb());
    }


    @ParameterizedTest
    @ValueSource(strings = {"help", "HELP", "hElP"})
    void shouldReturnCommandByForKeyExtractedFromTokenizedCommandLine(final String commandName) throws InvalidCommandNameException, UnknownCommandException {
        final var c = sut.getCommandFromString(commandName + " bla bla bla");

        assertNotNull(c);
        assertEquals(CommandVerb.HELP, c.getVerb());
    }

    @Test
    void shouldThrowExceptionWhenCommandIsNotKnown() {
        assertThrows(UnknownCommandException.class, () -> sut.getCommandFromString("foobar"));
    }

    @Test
    void shouldThrowExceptionWhenCommandIsNotValid() {
        assertThrows(InvalidCommandNameException.class, () -> sut.getCommandFromString(null));
        assertThrows(InvalidCommandNameException.class, () -> sut.getCommandFromString("foo"));
    }
}