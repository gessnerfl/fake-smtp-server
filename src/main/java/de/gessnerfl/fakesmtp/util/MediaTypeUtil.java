package de.gessnerfl.fakesmtp.util;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.servlet.ServletContext;

@Service
public class MediaTypeUtil {

    public MediaType getMediaTypeForFileName(ServletContext servletContext, String fileName) {
        String mineType = servletContext.getMimeType(fileName);
        try {
            MediaType mediaType = MediaType.parseMediaType(mineType);
            return mediaType;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

}
