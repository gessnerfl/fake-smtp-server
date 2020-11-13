package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailAttachment;
import de.gessnerfl.fakesmtp.repository.EmailAttachmentRepository;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import de.gessnerfl.fakesmtp.util.MediaTypeUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import javax.servlet.ServletContext;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailRestControllerTest {
    @Mock
    private EmailRepository emailRepository;
    @Mock
    private EmailAttachmentRepository emailAttachmentRepository;
    @Mock
    private MediaTypeUtil mediaTypeUtil;
    @Mock
    private ServletContext servletContext;

    @InjectMocks
    private EmailRestController sut;

    @Test
    void shouldReturnListOfEmails() {
        final Page<Email> page = createFirstPageEmail();
        when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);

        var result = sut.all(0, 5, Sort.Direction.DESC);

        assertEquals(page.getContent(), result);
        verify(emailRepository).findAll(argThat(matchPageable(0, 5)));
        verifyNoMoreInteractions(emailRepository);
    }

    @Test
    void shouldReturnSingleEmailWhenIdIsValid() {
        var id = 12L;
        var mail = mock(Email.class);
        when(emailRepository.findById(id)).thenReturn(Optional.of(mail));

        var result = sut.getEmailById(id);

        assertEquals(mail, result);
        verify(emailRepository).findById(id);
    }

    private Page<Email> createFirstPageEmail() {
        var page = mock(Page.class);
        when(page.getNumber()).thenReturn(0);
        return page;
    }

    private ArgumentMatcher<Pageable> matchPageable(int page, int size) {
        return (item) -> item.getPageNumber() == page && item.getPageSize() == size;
    }

    @Test
    void shouldReturnResponseEntityForAttachment() {
        var fileContent = "this is the file content".getBytes(StandardCharsets.UTF_8);
        var filename = "myfile.txt";
        var emailId = 123L;
        var attachmentId = 456L;
        var email = mock(Email.class);
        var attachment = mock(EmailAttachment.class);
        var mediaType = MediaType.TEXT_PLAIN;

        when(email.getId()).thenReturn(emailId);
        when(attachment.getEmail()).thenReturn(email);
        when(attachment.getFilename()).thenReturn(filename);
        when(attachment.getData()).thenReturn(fileContent);
        when(emailAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(mediaTypeUtil.getMediaTypeForFileName(servletContext, filename)).thenReturn(mediaType);

        var result = sut.getEmailAttachmentById(emailId, attachmentId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("attachment;filename=myfile.txt", result.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION).get(0));
        assertEquals(mediaType.toString(), result.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
        assertEquals(fileContent.length + "", result.getHeaders().get(HttpHeaders.CONTENT_LENGTH).get(0));
        assertArrayEquals(fileContent, result.getBody().getByteArray());
    }

    @Test
    void shouldThrowExceptionWhenNoAttachmentExistsForTheGivenId() {
        var emailId = 123L;
        var attachmentId = 456L;
        var email = mock(Email.class);
        var attachment = mock(EmailAttachment.class);

        when(email.getId()).thenReturn(789L);
        when(attachment.getEmail()).thenReturn(email);
        when(emailAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        assertThrows(AttachmentNotFoundException.class, () -> {
            sut.getEmailAttachmentById(emailId, attachmentId);
        });
    }

    @Test
    void shouldThrowExceptionWhenAttachmentExistsForTheGivenIdButTheEmailIdDoesNotMatch() {
        var emailId = 123L;
        var attachmentId = 456L;

        when(emailAttachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        assertThrows(AttachmentNotFoundException.class, () -> {
            sut.getEmailAttachmentById(emailId, attachmentId);
        });
    }

    @Test
    void shouldDeleteEmailByItsIdAndFlushChangesSoThatDeleteIsApplied() {
        var emailId = 123L;

        sut.deleteEmailById(emailId);

        verify(emailRepository).deleteById(emailId);
        verify(emailRepository).flush();
    }
}