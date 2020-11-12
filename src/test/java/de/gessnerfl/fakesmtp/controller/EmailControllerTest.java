package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.info.BuildProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EmailControllerTest {
    @Mock
    private Model model;
    @Mock
    private EmailRepository emailRepository;
    @Mock
    private BuildProperties buildProperties;
    @InjectMocks
    private EmailController sut;

    @Test
    public void shouldReturnEmailsPaged() {
        final String appVersion = "appVersion";
        final Page<Email> page = createFirstPageEmail();
        when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(buildProperties.getVersion()).thenReturn(appVersion);

        var result = sut.getAll(0, 5, model);

        assertEquals(EmailController.EMAIL_LIST_VIEW, result);

        verify(emailRepository).findAll(argThat(matchPageable(0, 5)));
        verify(model).addAttribute(EmailController.EMAIL_LIST_MODEL_NAME, page);
        verify(buildProperties).getVersion();
        verify(model).addAttribute(EmailController.APP_VERSION_MODEL_NAME, appVersion);
        verifyNoMoreInteractions(emailRepository, buildProperties, model);
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
        verifyNoMoreInteractions(emailRepository, buildProperties, model);
    }

    @Test
    public void shouldNotRedirectToFirstPageWhenNoDataIsAvailable() {
        final String appVersion = "appVersion";
        var page = mock(Page.class);
        when(page.getNumber()).thenReturn(0);
        when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(buildProperties.getVersion()).thenReturn(appVersion);

        var result = sut.getAll(0, 5, model);

        assertEquals(EmailController.EMAIL_LIST_VIEW, result);

        verify(emailRepository).findAll(argThat(matchPageable(0, 5)));
        verify(model).addAttribute(EmailController.EMAIL_LIST_MODEL_NAME, page);
        verify(buildProperties).getVersion();
        verify(model).addAttribute(EmailController.APP_VERSION_MODEL_NAME, appVersion);
        verifyNoMoreInteractions(emailRepository, emailRepository, model);
    }

    @Test
    public void shouldRedirectToFirstPageWhenPageNumberIsBelowNull() {
        var result = sut.getAll(-1, 5, model);

        assertEquals(EmailController.REDIRECT_EMAIL_LIST_VIEW, result);
        verifyNoInteractions(emailRepository, buildProperties, model);
    }

    @Test
    public void shouldRedirectToFirstPageWhenPageSizeIsNull() {
        String result = sut.getAll(0, 0, model);

        assertEquals(EmailController.REDIRECT_EMAIL_LIST_VIEW, result);
        verifyNoInteractions(emailRepository, buildProperties, model);
    }

    @Test
    public void shouldRedirectToFirstPageWhenPageSizeIsBelowNull() {
        var result = sut.getAll(0, -1, model);

        assertEquals(EmailController.REDIRECT_EMAIL_LIST_VIEW, result);
        verifyNoInteractions(emailRepository, buildProperties, model);
    }

    @Test
    public void shouldReturnSingleEmailWhenIdIsValid() {
        final String appVersion = "appVersion";
        var id = 12L;
        var mail = mock(Email.class);
        when(emailRepository.findById(id)).thenReturn(Optional.of(mail));
        when(buildProperties.getVersion()).thenReturn(appVersion);

        var result = sut.getEmailById(id, model);

        assertEquals(EmailController.SINGLE_EMAIL_VIEW, result);

        verify(emailRepository).findById(id);
        verify(model).addAttribute(EmailController.SINGLE_EMAIL_MODEL_NAME, mail);
        verify(buildProperties).getVersion();
        verify(model).addAttribute(EmailController.APP_VERSION_MODEL_NAME, appVersion);
        verifyNoMoreInteractions(emailRepository, buildProperties, model);
    }

    @Test
    public void shouldReturnRedirectToListPageWhenIdIsNotValid() {
        var id = 12L;
        when(emailRepository.findById(id)).thenReturn(Optional.empty());

        var result = sut.getEmailById(id, model);

        assertEquals(EmailController.REDIRECT_EMAIL_LIST_VIEW, result);

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
    public void shouldDeleteEmailByItsIdAndFlushChangesSoThatDeleteIsApplied(){
        var emailId = 123L;

        sut.deleteEmailById(emailId);

        verify(emailRepository).deleteById(emailId);
        verify(emailRepository).flush();
        verifyNoMoreInteractions(emailRepository);
        verifyNoInteractions(buildProperties);
    }
}