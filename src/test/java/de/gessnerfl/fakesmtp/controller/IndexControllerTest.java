package de.gessnerfl.fakesmtp.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IndexControllerTest {

    @Test
    void shouldReturnIndexPage(){
        assertEquals("index.html", new IndexController().home());
    }

}