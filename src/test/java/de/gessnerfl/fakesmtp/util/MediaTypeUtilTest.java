package de.gessnerfl.fakesmtp.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import javax.servlet.ServletContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MediaTypeUtilTest {
    private ServletContext servletContext;

    private MediaTypeUtil sut;

    @BeforeEach
    void init(){
        servletContext = mock(ServletContext.class);

        sut = new MediaTypeUtil();
    }

    @Test
    void shouldReturnMediaTypeForDefaultMimeType(){
        var filename = "mypicture.png";

        when(servletContext.getMimeType(filename)).thenReturn(MediaType.IMAGE_PNG_VALUE);

        var result = sut.getMediaTypeForFileName(servletContext, filename);

        assertEquals(MediaType.IMAGE_PNG, result);
    }

    @Test
    void shouldReturnMappedMimeTimeForNonDefaultMediaType(){
        var filename = "my-word-file.docx";

        when(servletContext.getMimeType(filename)).thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        var result = sut.getMediaTypeForFileName(servletContext, filename);

        assertEquals(new MediaType("application", "vnd.openxmlformats-officedocument.wordprocessingml.document"), result);
    }

    @Test
    void shouldReturnOctedStreamForInvalidMediaType(){
        var filename = "my-word-file.foo";

        when(servletContext.getMimeType(filename)).thenReturn("invalidMediaType");

        var result = sut.getMediaTypeForFileName(servletContext, filename);

        assertEquals(MediaType.APPLICATION_OCTET_STREAM, result);
    }

}