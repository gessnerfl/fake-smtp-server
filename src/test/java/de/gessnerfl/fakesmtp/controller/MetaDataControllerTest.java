package de.gessnerfl.fakesmtp.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetaDataControllerTest {

    @Mock
    private BuildProperties buildProperties;
    @InjectMocks
    private MetaDataController sut;

    @Test
    void shouldReturnApplicationVersion() {
        final var version = "app-version";
        when(buildProperties.getVersion()).thenReturn(version);

        final var meta = sut.get();

        assertEquals(version, meta.getVersion());
        verify(buildProperties).getVersion();
    }

}