package de.gessnerfl.fakesmtp.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class UiRouteControllerTest {

    private UiRouteController sut;

    @BeforeEach
    void init(){
        sut = new UiRouteController();
    }

    @Test
    void shouldReturnIndexForBasePath(){
        assertEquals("index", sut.index());
    }

    @Test
    void shouldReturnIndexPageForEmailsRoutes(){
        assertEquals("index", sut.emails(mock(HttpServletRequest.class)));
    }

}