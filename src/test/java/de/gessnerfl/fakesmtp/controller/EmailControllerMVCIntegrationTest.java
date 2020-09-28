package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailAttachment;
import de.gessnerfl.fakesmtp.model.EmailContent;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("integrationtest")
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class EmailControllerMVCIntegrationTest {

    @Autowired
    private EmailRepository emailRepository;
    @Autowired
    private MockMvc mockMvc;

    @Before
    public void init(){
        emailRepository.deleteAll();
    }

    @Test
    public void shouldReturnEmptyListWhenNoEmailsAreAvailable() throws Exception {
        this.mockMvc.perform(get("/email?page"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("mails", emptyIterableOf(Email.class)))
                .andExpect(view().name("email-list"));
    }

    @Test
    public void shouldReturnListOfEmailsPagedWhenEmailsAreAvailable() throws Exception {
        var email1 = createRandomEmail(5);
        var email2 = createRandomEmail(2);
        var email3 = createRandomEmail(1);

        this.mockMvc.perform(get("/email?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("mails", iterableWithSize(2)))
                .andExpect(model().attribute("mails", contains(equalTo(email3), equalTo(email2))))
                .andExpect(view().name("email-list"));

        this.mockMvc.perform(get("/email?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("mails", iterableWithSize(1)))
                .andExpect(model().attribute("mails", contains(equalTo(email1))))
                .andExpect(view().name("email-list"));
    }

    @Test
    public void shouldReturnFirstPageWhenGivenPageIsOutOfRange() throws Exception {
        createRandomEmail(1);

        this.mockMvc.perform(get("/email?page=1&size=2"))
                .andExpect(status().isFound())
                .andExpect(model().attributeDoesNotExist("mails"))
                .andExpect(redirectedUrl("/email"));
    }

    @Test
    public void shouldReturnMailById() throws Exception {
        var email = createRandomEmail(1);

        this.mockMvc.perform(get("/email/"+email.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("mail", equalTo(email)))
                .andExpect(view().name("email"));
    }

    @Test
    public void shouldReturnErrorWhenMailIdIsNotValid() throws Exception {
        this.mockMvc.perform(get("/email/123"))
                .andExpect(redirectedUrl("/email"))
                .andExpect(model().attributeDoesNotExist("mails", "mail"))
                .andExpect(status().isFound());
    }

    @Test
    public void shouldDeleteEmail() throws Exception {
        var email = createRandomEmail(1);

        this.mockMvc.perform(delete("/email/"+email.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/email")).andExpect(status().isFound());

        assertThat(emailRepository.findAll(), empty());
    }

    private Email createRandomEmail(int minusMinutes) {
        return emailRepository.save(EmailControllerUtil.prepareRandomEmail(minusMinutes));
    }

}