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
import org.springframework.web.servlet.ModelAndView;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EmailControllerTest {

    @Mock
    private EmailRepository emailRepository;
    @InjectMocks
    private EmailController sut;

    @Test
    public void shouldReturnEmailsPaged() {
        final Page<Email> page = createFirstPageEmail();
        when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);

        ModelAndView result = sut.getAll(0, 5);

        assertSame(page, result.getModel().get(EmailController.EMAIL_LIST_MODEL_NAME));
        assertNull(result.getModel().get(EmailController.ERROR_MODEL_NAME));
        assertEquals(EmailController.EMAIL_LIST_VIEW, result.getViewName());

        verify(emailRepository).findAll(argThat(matchPageable(0, 5)));
    }

    @Test
    public void shouldReturnFirstPageWhenRequestedPageIsOutOfRange() {
        final Page<Email> page = mock(Page.class);
        when(page.getTotalElements()).thenReturn(8L);
        when(page.getTotalPages()).thenReturn(2);
        when(page.getNumber()).thenReturn(3, 0);
        when(page.getNumberOfElements()).thenReturn(0, 5);
        when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);

        ModelAndView result = sut.getAll(2, 5);

        Page<Email> mails = (Page) result.getModel().get(EmailController.EMAIL_LIST_MODEL_NAME);
        assertSame(page, mails);
        assertEquals(0, page.getNumber());
        assertNull(result.getModel().get(EmailController.ERROR_MODEL_NAME));
        assertEquals(EmailController.EMAIL_LIST_VIEW, result.getViewName());

        verify(emailRepository).findAll(argThat(matchPageable(2, 5)));
        verify(emailRepository).findAll(argThat(matchPageable(0, 5)));
    }

    @Test
    public void shouldNormalizePageNumberToAPositiveInteger(){
        final Page<Email> page = createFirstPageEmail();
        when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);

        sut.getAll(-1, 5);

        verify(emailRepository).findAll(argThat(matchPageable(0, 5)));
    }

    @Test
    public void shouldNormalizePageSizeToAPositiveInteger(){
        final Page<Email> page = createFirstPageEmail();
        when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);

        sut.getAll(0, -1);

        verify(emailRepository).findAll(argThat(matchPageable(0, EmailController.DEFAULT_PAGE_SIZE)));
    }

    @Test
    public void shouldReturnSingleEmailWhenIdIsValid(){
        final long id = 12L;
        final Email mail = mock(Email.class);
        when(emailRepository.findOne(id)).thenReturn(mail);

        ModelAndView result = sut.getEmailById(id);

        assertSame(mail, result.getModel().get(EmailController.SINGLE_EMAIL_MODEL_NAME));
        assertNull(result.getModel().get(EmailController.ERROR_MODEL_NAME));
        assertEquals(EmailController.SINGLE_EMAIL_VIEW, result.getViewName());

        verify(emailRepository).findOne(id);
        verify(emailRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    public void shouldReturnMainPageWithErrorMessageWhenIdIsNotValid(){
        final long id = 12L;
        final Page<Email> page = createFirstPageEmail();
        when(emailRepository.findOne(id)).thenReturn(null);
        when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);

        ModelAndView result = sut.getEmailById(id);

        assertNull(result.getModel().get(EmailController.SINGLE_EMAIL_MODEL_NAME));
        assertSame(page, result.getModel().get(EmailController.EMAIL_LIST_MODEL_NAME));
        assertEquals(Boolean.TRUE, result.getModel().get(EmailController.ERROR_MODEL_NAME));
        assertEquals("Email with ID 12 does not exist.",  result.getModel().get(EmailController.ERROR_MESSAGE_MODEL_NAME));
        assertEquals(EmailController.EMAIL_LIST_VIEW, result.getViewName());

        verify(emailRepository).findOne(id);
        verify(emailRepository).findAll(any(Pageable.class));
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