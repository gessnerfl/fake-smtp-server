package de.gessnerfl.fakesmtp.util;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.servlet.ServletContext;

@Service
public class MediaTypeUtil {

    public MediaType getMediaTypeForFileName(ServletContext servletContext, String fileName) {
        var mineType = servletContext.getMimeType(fileName);
        try {
            return MediaType.parseMediaType(mineType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

}
