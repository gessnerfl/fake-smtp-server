package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailAttachment;
import de.gessnerfl.fakesmtp.repository.EmailAttachmentRepository;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import de.gessnerfl.fakesmtp.util.MediaTypeUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;

import javax.servlet.ServletContext;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EmailControllerTest {
    @Mock
    private Model model;
    @Mock
    private EmailRepository emailRepository;
    @Mock
    private EmailAttachmentRepository emailAttachmentRepository;
    @Mock
    private MediaTypeUtil mediaTypeUtil;
    @Mock
    private ServletContext servletContext;

    @InjectMocks
    private EmailController sut;

    @Test
    public void shouldReturnEmailsPaged() {
        final Page<Email> page = createFirstPageEmail();
        when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);

        var result = sut.getAll(0, 5, model);

        assertEquals(EmailController.EMAIL_LIST_VIEW, result);

        verify(emailRepository).findAll(argThat(matchPageable(0, 5)));
        verifyNoMoreInteractions(emailRepository);
        verify(model).addAttribute(EmailController.EMAIL_LIST_MODEL_NAME, page);
    }

    @Test
    public void shouldReturnRedirectToFirstPageWhenRequestedPageIsOutOfRange() {
        var page = mock(Page.class);
        when(page.getTotalPages()).thenReturn(2);
        when(page.getNumber()).thenReturn(3);
        when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);

        var result = sut.getAll(3, 5, model);

        assertEquals(EmailController.REDIRECT_EMAIL_LIST_VIEW, result);

        verify(emailRepository).findAll(argThat(matchPageable(3, 5)));
        verifyNoMoreInteractions(emailRepository);
    }

    @Test
    public void shouldNotRedirectToFirstPageWhenNoDataIsAvailable() {
        var page = mock(Page.class);
        when(page.getNumber()).thenReturn(0);
        when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);

        var result = sut.getAll(0, 5, model);

        assertEquals(EmailController.EMAIL_LIST_VIEW, result);

        verify(emailRepository).findAll(argThat(matchPageable(0, 5)));
        verifyNoMoreInteractions(emailRepository);
    }

    @Test
    public void shouldRedirectToFirstPageWhenPageNumberIsBelowNull() {
        var result = sut.getAll(-1, 5, model);

        assertEquals(EmailController.REDIRECT_EMAIL_LIST_VIEW, result);
        verifyZeroInteractions(emailRepository);
    }

    @Test
    public void shouldRedirectToFirstPageWhenPageSizeIsNull() {
        String result = sut.getAll(0, 0, model);

        assertEquals(EmailController.REDIRECT_EMAIL_LIST_VIEW, result);
        verifyZeroInteractions(emailRepository);
    }

    @Test
    public void shouldRedirectToFirstPageWhenPageSizeIsBelowNull() {
        var result = sut.getAll(0, -1, model);

        assertEquals(EmailController.REDIRECT_EMAIL_LIST_VIEW, result);
        verifyZeroInteractions(emailRepository);
    }

    @Test
    public void shouldReturnSingleEmailWhenIdIsValid() {
        var id = 12L;
        var mail = mock(Email.class);
        when(emailRepository.findById(id)).thenReturn(Optional.of(mail));

        var result = sut.getEmailById(id, model);

        assertEquals(EmailController.SINGLE_EMAIL_VIEW, result);

        verify(emailRepository).findById(id);
        verify(model).addAttribute(EmailController.SINGLE_EMAIL_MODEL_NAME, mail);
    }

    @Test
    public void shouldReturnRedirectToListPageWhenIdIsNotValid() {
        var id = 12L;
        when(emailRepository.findById(id)).thenReturn(Optional.empty());

        var result = sut.getEmailById(id, model);

        assertEquals(EmailController.REDIRECT_EMAIL_LIST_VIEW, result);

        verify(emailRepository).findById(id);
    }

    private Page<Email> createFirstPageEmail() {
        var page = mock(Page.class);
        when(page.getNumber()).thenReturn(0);
        return page;
    }


    private ArgumentMatcher<Pageable> matchPageable(int page, int size) {
        return (item) ->  item.getPageNumber() == page && item.getPageSize() == size;
    }

    @Test
    public void shouldReturnResponseEntityForAttachment(){
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
        assertEquals(fileContent.length+"", result.getHeaders().get(HttpHeaders.CONTENT_LENGTH).get(0));
        assertArrayEquals(fileContent, result.getBody().getByteArray());
    }

    @Test(expected = AttachmentNotFoundException.class)
    public void shouldThrowExceptionWhenNoAttachmentExistsForTheGivenId(){
        var emailId = 123L;
        var attachmentId = 456L;
        var email = mock(Email.class);
        var attachment = mock(EmailAttachment.class);

        when(email.getId()).thenReturn(789L);
        when(attachment.getEmail()).thenReturn(email);
        when(emailAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        sut.getEmailAttachmentById(emailId, attachmentId);
    }

    @Test(expected = AttachmentNotFoundException.class)
    public void shouldThrowExceptionWhenAttachmentExistsForTheGivenIdButTheEmailIdDoesNotMatch(){
        var emailId = 123L;
        var attachmentId = 456L;

        when(emailAttachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        sut.getEmailAttachmentById(emailId, attachmentId);
    }
}