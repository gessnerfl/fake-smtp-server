package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EmailControllerTest {
    @Mock
    private Model model;
    @Mock
    private EmailRepository emailRepository;
    @InjectMocks
    private EmailController sut;

    @Test
    public void shouldReturnEmailsPaged() {
        final Page<Email> page = createFirstPageEmail();
        when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);

        String result = sut.getAll(0, 5, model);

        assertEquals(EmailController.EMAIL_LIST_VIEW, result);

        verify(emailRepository).findAll(argThat(matchPageable(0, 5)));
        verifyNoMoreInteractions(emailRepository);
        verify(model).addAttribute(EmailController.EMAIL_LIST_MODEL_NAME, page);
    }

    @Test
    public void shouldReturnRedirectToFirstPageWhenRequestedPageIsOutOfRange() {
        final Page<Email> page = mock(Page.class);
        when(page.getTotalElements()).thenReturn(8L);
        when(page.getTotalPages()).thenReturn(2);
        when(page.getNumber()).thenReturn(3, 0);
        when(page.getNumberOfElements()).thenReturn(0, 5);
        when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);

        String result = sut.getAll(2, 5, model);

        assertEquals(EmailController.REDIRECT_EMAIL_LIST_VIEW, result);

        verify(emailRepository).findAll(argThat(matchPageable(2, 5)));
        verifyNoMoreInteractions(emailRepository);
    }

    @Test
    public void shouldRedirectToFirstPageWhenPageNumberIsBelowNull() {
        String result = sut.getAll(-1, 5, model);

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
        String result = sut.getAll(0, -1, model);

        assertEquals(EmailController.REDIRECT_EMAIL_LIST_VIEW, result);
        verifyZeroInteractions(emailRepository);
    }

    @Test
    public void shouldReturnSingleEmailWhenIdIsValid() {
        final long id = 12L;
        final Email mail = mock(Email.class);
        when(emailRepository.findOne(id)).thenReturn(mail);

        String result = sut.getEmailById(id, model);

        assertEquals(EmailController.SINGLE_EMAIL_VIEW, result);

        verify(emailRepository).findOne(id);
        verify(model).addAttribute(EmailController.SINGLE_EMAIL_MODEL_NAME, mail);
    }

    @Test
    public void shouldReturnRedirectToListPageWhenIdIsNotValid() {
        final long id = 12L;
        final Page<Email> page = createFirstPageEmail();
        when(emailRepository.findOne(id)).thenReturn(null);
        when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);

        String result = sut.getEmailById(id, model);

        assertEquals(EmailController.REDIRECT_EMAIL_LIST_VIEW, result);

        verify(emailRepository).findOne(id);
    }

    private Page<Email> createFirstPageEmail() {
        Page<Email> page = mock(Page.class);
        when(page.getTotalElements()).thenReturn(8L);
        when(page.getTotalPages()).thenReturn(2);
        when(page.getNumber()).thenReturn(0);
        when(page.getNumberOfElements()).thenReturn(5);
        return page;
    }


    private Matcher<Pageable> matchPageable(int page, int size) {
        return new BaseMatcher<Pageable>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("Pagable should have pageNumber ").appendValue(page).appendText(" and pageSize ").appendValue(size);
            }

            @Override
            public boolean matches(Object item) {
                if (item instanceof Pageable) {
                    Pageable p = (Pageable) item;
                    return p.getPageNumber() == page && p.getPageSize() == size;
                }
                return false;
            }
        };
    }
}