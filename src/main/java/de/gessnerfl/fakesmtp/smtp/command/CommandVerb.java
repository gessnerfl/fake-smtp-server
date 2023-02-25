package de.gessnerfl.fakesmtp.smtp.command;

public enum CommandVerb {
    AUTH,
    DATA,
    EHLO,
    HELO,
    HELP,
    MAIL,
    NOOP,
    QUIT,
    RCPT,
    RSET,
    STARTTLS,
    VRFY,
    EXPN;
}
