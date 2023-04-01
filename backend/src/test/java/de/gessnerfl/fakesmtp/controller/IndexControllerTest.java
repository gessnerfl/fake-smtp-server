package de.gessnerfl.fakesmtp.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IndexControllerTest {

    @Test
    public void shouldReturnIndexPage(){
        assertEquals("index", new IndexController().home());
    }

}