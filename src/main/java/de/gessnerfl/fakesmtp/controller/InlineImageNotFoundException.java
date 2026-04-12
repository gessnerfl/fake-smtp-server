package de.gessnerfl.fakesmtp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class InlineImageNotFoundException extends RuntimeException {

    public InlineImageNotFoundException(String message) {
        super(message);
    }
}
