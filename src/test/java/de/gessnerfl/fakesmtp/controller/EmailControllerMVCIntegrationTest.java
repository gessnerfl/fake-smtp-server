package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("integrationtest")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class EmailControllerMVCIntegrationTest {

    @Autowired
    private EmailRepository emailRepository;
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void init(){
        emailRepository.deleteAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoEmailsAreAvailable() throws Exception {
        this.mockMvc.perform(get("/email?page"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("mails", emptyIterableOf(Email.class)))
                .andExpect(model().attribute("appVersion", any(String.class)))
                .andExpect(view().name("email-list"));
    }

    @Test
    void shouldReturnListOfEmailsPagedWhenEmailsAreAvailable() throws Exception {
        var email1 = createRandomEmail(5);
        var email2 = createRandomEmail(2);
        var email3 = createRandomEmail(1);

        this.mockMvc.perform(get("/email?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("mails", iterableWithSize(2)))
                .andExpect(model().attribute("mails", contains(equalTo(email3), equalTo(email2))))
                .andExpect(model().attribute("appVersion", any(String.class)))
                .andExpect(view().name("email-list"));

        this.mockMvc.perform(get("/email?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("mails", iterableWithSize(1)))
                .andExpect(model().attribute("mails", contains(equalTo(email1))))
                .andExpect(model().attribute("appVersion", any(String.class)))
                .andExpect(view().name("email-list"));
    }

    @Test
    void shouldReturnFirstPageWhenGivenPageIsOutOfRange() throws Exception {
        createRandomEmail(1);

        this.mockMvc.perform(get("/email?page=1&size=2"))
                .andExpect(status().isFound())
                .andExpect(model().attributeDoesNotExist("mails"))
                .andExpect(redirectedUrl("/email"));
    }

    @Test
    void shouldReturnMailById() throws Exception {
        var email = createRandomEmail(1);

        this.mockMvc.perform(get("/email/"+email.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("mail", equalTo(email)))
                .andExpect(model().attribute("appVersion", any(String.class)))
                .andExpect(view().name("email"));
    }

    @Test
    void shouldReturnErrorWhenMailIdIsNotValid() throws Exception {
        this.mockMvc.perform(get("/email/123"))
                .andExpect(redirectedUrl("/email"))
                .andExpect(model().attributeDoesNotExist("mails", "mail"))
                .andExpect(status().isFound());
    }

    @Test
    void shouldDeleteEmail() throws Exception {
        var email = createRandomEmail(1);

        this.mockMvc.perform(delete("/email/"+email.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/email")).andExpect(status().isFound());

        assertThat(emailRepository.findAll(), empty());
    }

    @Test
    void shouldDeleteAllEmails() throws Exception {
        var email = createRandomEmails(5, 1);

        assertThat(emailRepository.findAll(), hasSize(5));

        this.mockMvc.perform(delete("/email"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/email")).andExpect(status().isFound());

        assertThat(emailRepository.findAll(), empty());
    }

    private List<Email> createRandomEmails(int numberOfEmails, int minusMinutes) {
        return IntStream.range(0, numberOfEmails).mapToObj(i -> createRandomEmail(minusMinutes)).collect(Collectors.toList());
    }

    private Email createRandomEmail(int minusMinutes) {
        return emailRepository.save(EmailControllerUtil.prepareRandomEmail(minusMinutes));
    }

}