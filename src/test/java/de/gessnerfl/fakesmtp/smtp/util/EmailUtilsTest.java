package de.gessnerfl.fakesmtp.smtp.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailUtilsTest {

    @Test
    void shouldReturnEmptyStringWhenOffsetExceedsArgsLength() {
        String result = assertDoesNotThrow(() -> EmailUtils.extractEmailAddress("", 3));

        assertEquals("", result);
    }

    @Test
    void shouldExtractAddressEvenWhenClosingBracketIsMissing() {
        String result = assertDoesNotThrow(() -> EmailUtils.extractEmailAddress("TO:<user@example.com", 3));

        assertEquals("user@example.com", result);
    }

    @Test
    void shouldExtractAddressWithinAngleBrackets() {
        String result = EmailUtils.extractEmailAddress("TO:< user@example.com >", 3);

        assertEquals("user@example.com", result);
    }
}
