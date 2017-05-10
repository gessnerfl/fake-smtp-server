package de.gessnerfl.fakesmtp;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertNotNull;

public class TestResourceUtil {
    private static final String TEST_DATA_FOLDER = "/test-data/";

    public static String getTestFileContent(String testFilename) throws IOException {
        return IOUtils.toString(getTestFile(testFilename), StandardCharsets.UTF_8);
    }

    public static InputStream getTestFile(String filename) {
        InputStream stream = TestResourceUtil.class.getResourceAsStream(TEST_DATA_FOLDER + filename);
        if (stream == null) {
            stream = TestResourceUtil.class.getClassLoader().getResourceAsStream(TEST_DATA_FOLDER + filename);
        }
        assertNotNull(stream);
        return stream;
    }
}
