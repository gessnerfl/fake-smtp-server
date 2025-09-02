package de.gessnerfl.fakesmtp.smtp.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CRLFTerminatedReaderTest {

    @Test
    void shouldSuccessfullyReadLine() throws IOException {
        final var line1 = "This is the first line";
        final var line2 = "This is the second line";
        final var input = (line1 + "\r\n" + line2 + "\r\n").getBytes(StandardCharsets.UTF_8);
        try (var sut = new CRLFTerminatedReader(new ByteArrayInputStream(input), StandardCharsets.UTF_8)) {
            var result = new ArrayList<>();
            String line;
            while ((line = sut.readLine()) != null) {
                result.add(line);
            }
            assertThat(result, hasSize(2));
            assertEquals(line1, result.get(0));
            assertEquals(line2, result.get(1));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "\r", "\n" })
    void shouldTerminateWhenSingleCrLfIsProvided(String linebreak) throws IOException {
        assertThrows(CRLFTerminatedReader.TerminationException.class, () -> {
            final var line1 = "This is the first line";
            final var line2 = "This is the second line";
            final var input = (line1 + linebreak + line2 + "\r\n").getBytes(StandardCharsets.UTF_8);
            try (var sut = new CRLFTerminatedReader(new ByteArrayInputStream(input), StandardCharsets.UTF_8)) {
                while (sut.readLine() != null) {
                    // Required to read
                }
                fail();
            }
        });
    }

    @Test
    void shouldTerminateWhenMultipleCrLfAreProvided() throws IOException {
        assertThrows(CRLFTerminatedReader.TerminationException.class, () -> {
            final var line1 = "This is the first line";
            final var line2 = "This is the second line";
            final var input = (line1 + "\r\r\n" + line2 + "\r\n").getBytes(StandardCharsets.UTF_8);
            try (var sut = new CRLFTerminatedReader(new ByteArrayInputStream(input), StandardCharsets.UTF_8)) {
                while (sut.readLine() != null) {
                    // Required to read
                }
                fail();
            }
        });
    }

    @Test
    void shouldFailToReadLineWhenMaxNumberOfCharactersIsReached() {
        assertThrows(CRLFTerminatedReader.MaxLineLengthException.class, () -> {
            final var line1 = RandomStringUtils.insecure().nextAlphanumeric(CRLFTerminatedReader.MAX_LINE_LENGTH + 1);
            final var input = (line1 + "\r\n").getBytes(StandardCharsets.UTF_8);
            try (var sut = new CRLFTerminatedReader(new ByteArrayInputStream(input), StandardCharsets.UTF_8)) {
                while (sut.readLine() != null) {
                    // Required to read
                }
                fail();
            }
        });
    }

}
