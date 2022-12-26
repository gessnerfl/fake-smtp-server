package de.gessnerfl.fakesmtp.server.smtp.util;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

public class EmailUtils {

    private EmailUtils() {
    }

    /**
     * @return true if the string is a valid email address
     */
    public static boolean isValidEmailAddress(final String address) {
        // MAIL FROM: <>
        if (address.length() == 0) {
            return true;
        }

        boolean result = false;
        try {
            final InternetAddress[] ia = InternetAddress.parse(address, true);
            if (ia.length == 0) {
                result = false;
            } else {
                result = true;
            }
        } catch (final AddressException ae) {
            result = false;
        }
        return result;
    }

    /**
     * Extracts the email address within a &lt;&gt; after a specified offset.
     */
    public static String extractEmailAddress(final String args, final int offset) {
        String address = args.substring(offset).trim();
        if (address.indexOf('<') == 0) {
            address = address.substring(1, address.indexOf('>'));
            // spaces within the <> are also possible, Postfix apparently
            // trims these away:
            return address.trim();
        }

        // find space (e.g. SIZE argument)
        final int nextarg = address.indexOf(" ");
        if (nextarg > -1) {
            address = address.substring(0, nextarg).trim();
        }
        return address;
    }
}
