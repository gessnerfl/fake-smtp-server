package de.gessnerfl.fakesmtp.smtp.io;

import de.gessnerfl.fakesmtp.TestResourceUtil;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DeferredFileOutputStreamTest {

    @Test
    void shouldReadWithoutWritingToFile() throws IOException {
        shouldReadFile(DataSize.ofMegabytes(1));
    }

    @Test
    void shouldReadWithWritingToFile() throws IOException {
        shouldReadFile(DataSize.ofBytes(512));
    }

    private void shouldReadFile(DataSize maxBufferSize) throws IOException {
        var testFilename = "mail-with-subect-and-content-type-html-with-inline-image.eml";
        var data = TestResourceUtil.getTestFileContentBytes(testFilename);
        var inputStream = new ByteArrayInputStream(data);

        try(var sut = new DeferredFileOutputStream((int)maxBufferSize.toBytes())){
            int value;
            while ((value = inputStream.read()) >= 0) {
                sut.write(value);
            }

            var result = IOUtils.toByteArray(sut.getInputStream());
            assertArrayEquals(data, result);
        }
    }
}