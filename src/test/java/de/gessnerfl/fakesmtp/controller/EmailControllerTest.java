package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.repository.EmailAttachmentRepository;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailControllerTest {
    @Mock
    private Model model;
    @Mock
    private EmailRepository emailRepository;
    @Mock
    private EmailAttachmentRepository emailAttachmentRepository;
    @Mock
    private BuildProperties buildProperties;
    @InjectMocks
    private EmailController sut;

    @Test
    void shouldReturnEmailsPaged() {
        final String appVersion = "appVersion";
        final Page<Email> page = createFirstPageEmail();
        when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(buildProperties.getVersion()).thenReturn(appVersion);

        var result = sut.getAll(0, 5, model);

        Assertions.assertEquals(EmailController.EMAIL_LIST_VIEW, result);

        verify(emailRepository).findAll(argThat(matchPageable(0, 5)));
        verify(model).addAttribute(EmailController.EMAIL_LIST_MODEL_NAME, page);
        verify(buildProperties).getVersion();
        verify(model).addAttribute(EmailController.APP_VERSION_MODEL_NAME, appVersion);
        verifyNoMoreInteractions(emailRepository, buildProperties, model);
    }

    @Test
    void shouldReturnRedirectToFirstPageWhenRequestedPageIsOutOfRange() {
        var page = mock(Page.class);
        when(page.getTotalPages()).thenReturn(2);
        when(page.getNumber()).thenReturn(3);
        when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);

        var result = sut.getAll(3, 5, model);

        Assertions.assertEquals(EmailController.REDIRECT_EMAIL_LIST_VIEW, result);

        verify(emailRepository).findAll(argThat(matchPageable(3, 5)));
        verifyNoMoreInteractions(emailRepository, buildProperties, model);
    }

    @Test
    void shouldNotRedirectToFirstPageWhenNoDataIsAvailable() {
        final String appVersion = "appVersion";
        var page = mock(Page.class);
        when(page.getNumber()).thenReturn(0);
        when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(buildProperties.getVersion()).thenReturn(appVersion);

        var result = sut.getAll(0, 5, model);

        Assertions.assertEquals(EmailController.EMAIL_LIST_VIEW, result);

        verify(emailRepository).findAll(argThat(matchPageable(0, 5)));
        verify(model).addAttribute(EmailController.EMAIL_LIST_MODEL_NAME, page);
        verify(buildProperties).getVersion();
        verify(model).addAttribute(EmailController.APP_VERSION_MODEL_NAME, appVersion);
        verifyNoMoreInteractions(emailRepository, emailRepository, model);
    }

    @Test
    void shouldRedirectToFirstPageWhenPageNumberIsBelowNull() {
        var result = sut.getAll(-1, 5, model);

        Assertions.assertEquals(EmailController.REDIRECT_EMAIL_LIST_VIEW, result);
        verifyNoInteractions(emailRepository, buildProperties, model);
    }

    @Test
    void shouldRedirectToFirstPageWhenPageSizeIsNull() {
        String result = sut.getAll(0, 0, model);

        Assertions.assertEquals(EmailController.REDIRECT_EMAIL_LIST_VIEW, result);
        verifyNoInteractions(emailRepository, buildProperties, model);
    }

    @Test
    void shouldRedirectToFirstPageWhenPageSizeIsBelowNull() {
        var result = sut.getAll(0, -1, model);

        Assertions.assertEquals(EmailController.REDIRECT_EMAIL_LIST_VIEW, result);
        verifyNoInteractions(emailRepository, buildProperties, model);
    }

    @Test
    void shouldReturnSingleEmailWhenIdIsValid() {
        final String appVersion = "appVersion";
        var id = 12L;
        var mail = mock(Email.class);
        when(emailRepository.findById(id)).thenReturn(Optional.of(mail));
        when(buildProperties.getVersion()).thenReturn(appVersion);

        var result = sut.getEmailById(id, model);

        Assertions.assertEquals(EmailController.SINGLE_EMAIL_VIEW, result);

        verify(emailRepository).findById(id);
        verify(model).addAttribute(EmailController.SINGLE_EMAIL_MODEL_NAME, mail);
        verify(buildProperties).getVersion();
        verify(model).addAttribute(EmailController.APP_VERSION_MODEL_NAME, appVersion);
        verifyNoMoreInteractions(emailRepository, buildProperties, model);
    }

    @Test
    void shouldReturnRedirectToListPageWhenIdIsNotValid() {
        var id = 12L;
        when(emailRepository.findById(id)).thenReturn(Optional.empty());

        var result = sut.getEmailById(id, model);

        Assertions.assertEquals(EmailController.REDIRECT_EMAIL_LIST_VIEW, result);

        verify(emailRepository).findById(id);
        verifyNoMoreInteractions(emailRepository);
        verifyNoInteractions(buildProperties, model);
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
    void shouldDeleteEmailByItsIdAndFlushChangesSoThatDeleteIsApplied(){
        var emailId = 123L;

        sut.deleteEmailById(emailId);

        verify(emailRepository).deleteById(emailId);
        verify(emailRepository).flush();
        verifyNoMoreInteractions(emailRepository);
        verifyNoInteractions(buildProperties);
    }

    @Test
    void shouldDeleteAllEmails(){
        sut.deleteAllEmails();

        verify(emailAttachmentRepository).deleteAllInBatch();
        verify(emailRepository).deleteAllInBatch();
        verify(emailRepository).flush();
        verifyNoMoreInteractions(emailRepository);
        verifyNoInteractions(buildProperties);
    }
}