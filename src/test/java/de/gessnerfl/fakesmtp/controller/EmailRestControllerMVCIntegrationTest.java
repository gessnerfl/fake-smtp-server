package de.gessnerfl.fakesmtp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("integrationtest")
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class EmailRestControllerMVCIntegrationTest {

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
        MvcResult mvcResult = mockMvc.perform(get("/api/email")).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        Email[] emails = mapFromJson(mvcResult.getResponse().getContentAsString(), Email[].class);
        assertEquals(0, emails.length);
    }

    @Test
    public void shouldReturnFirstPageOfEmails() throws Exception {
        var email1 = createRandomEmail(5);
        var email2 = createRandomEmail(2);
        var email3 = createRandomEmail(1);

        MvcResult mvcResult = this.mockMvc.perform(get("/api/email?page=0&size=2")).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        Email[] emails = mapFromJson(mvcResult.getResponse().getContentAsString(), Email[].class);
        assertEquals(List.of(email3, email2), List.of(emails));
    }

    @Test
    public void shouldReturnSecondPageOfEmails() throws Exception {
        var email1 = createRandomEmail(5);
        var email2 = createRandomEmail(2);
        var email3 = createRandomEmail(1);

        MvcResult mvcResult = this.mockMvc.perform(get("/api/email?page=1&size=2")).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        Email[] emails = mapFromJson(mvcResult.getResponse().getContentAsString(), Email[].class);
        assertEquals(List.of(email1), List.of(emails));
    }

    @Test
    public void shouldReturnNoEmailsWhenGivenPageIsOutOfRange() throws Exception {
        var email1 = createRandomEmail(5);

        MvcResult mvcResult = this.mockMvc.perform(get("/api/email?page=2&size=1")).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        Email[] emails = mapFromJson(mvcResult.getResponse().getContentAsString(), Email[].class);
        assertEquals(0, emails.length);
    }

    @Test
    public void shouldReturnMailById() throws Exception {
        var email = createRandomEmail(1);

        MvcResult mvcResult = this.mockMvc.perform(get("/api/email/"+email.getId())).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        Email actualEmail = mapFromJson(mvcResult.getResponse().getContentAsString(), Email.class);
        assertEquals(actualEmail, email);
    }

    @Test
    public void shouldReturnNotFoundCodeWhenMailIdIsNotValid() throws Exception {
        this.mockMvc.perform(get("/api/email/123"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnAttachmentForEmail() throws Exception {
        var email = createRandomEmail(1);
        var attachment = email.getAttachments().get(0);

        this.mockMvc.perform(get("/api/email/"+email.getId()+"/attachment/" + attachment.getId()))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,"attachment;filename=" + attachment.getFilename()))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH,"" + attachment.getData().length))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().bytes(attachment.getData()))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnErrorWhenAttachmentIsRequestedButAttachmentIdIsNotValid() throws Exception {
        var email = createRandomEmail(1);

        this.mockMvc.perform(get("/api/email/"+email.getId()+"/attachment/123"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnErrorWhenAttachmentIsRequestedButMailIdIsNotValid() throws Exception {
        var email = createRandomEmail(1);

        this.mockMvc.perform(get("/api/email/123/attachment/"+email.getAttachments().get(0).getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldDeleteEmail() throws Exception {
        var email = createRandomEmail(1);

        this.mockMvc.perform(delete("/api/email/"+email.getId()))
                .andExpect(status().is2xxSuccessful());

        assertThat(emailRepository.findAll(), empty());
    }

    private static <T> T mapFromJson(String json, Class<T> clazz) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, clazz);
    }

    private Email createRandomEmail(int minusMinutes) {
        return emailRepository.save(EmailControllerUtil.prepareRandomEmail(minusMinutes));
    }

}