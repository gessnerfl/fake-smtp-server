package de.gessnerfl.fakesmtp.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.RestResponsePage;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("integrationtest")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class EmailRestControllerMVCIntegrationTest {

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
        MvcResult mvcResult = mockMvc.perform(get("/api/email")).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        RestResponsePage<Email> emailPage = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {});
        assertEquals(0, emailPage.getNumber());
        assertEquals(0, emailPage.getNumberOfElements());
        assertEquals(0, emailPage.getTotalElements());
        assertEquals(0, emailPage.getContent().size());
    }

    @Test
    void shouldReturnFirstPageOfEmails() throws Exception {
        var email1 = createRandomEmail(5);
        var email2 = createRandomEmail(2);
        var email3 = createRandomEmail(1);

        MvcResult mvcResult = this.mockMvc.perform(get("/api/email?page=0&size=2")).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        RestResponsePage<Email> emailPage = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {});
        assertEquals(0, emailPage.getNumber());
        assertEquals(2, emailPage.getSize());
        assertEquals(2, emailPage.getTotalPages());
        assertEquals(2, emailPage.getNumberOfElements());
        assertEquals(3, emailPage.getTotalElements());
        assertEquals(List.of(email3, email2), emailPage.getContent());
    }

    @Test
    void shouldReturnSecondPageOfEmails() throws Exception {
        var email1 = createRandomEmail(5);
        var email2 = createRandomEmail(2);
        var email3 = createRandomEmail(1);

        MvcResult mvcResult = this.mockMvc.perform(get("/api/email?page=1&size=2")).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        RestResponsePage<Email> emailPage = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {});
        assertEquals(1, emailPage.getNumber());
        assertEquals(2, emailPage.getSize());
        assertEquals(2, emailPage.getTotalPages());
        assertEquals(1, emailPage.getNumberOfElements());
        assertEquals(3, emailPage.getTotalElements());
        assertEquals(List.of(email1), emailPage.getContent());
    }

    @Test
    void shouldReturnNoEmailsWhenGivenPageIsOutOfRange() throws Exception {
        var email1 = createRandomEmail(5);

        MvcResult mvcResult = this.mockMvc.perform(get("/api/email?page=2&size=1")).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        RestResponsePage<Email> emailPage = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {});
        assertEquals(2, emailPage.getNumber());
        assertEquals(1, emailPage.getSize());
        assertEquals(1, emailPage.getTotalPages());
        assertEquals(1, emailPage.getTotalElements());
        assertEquals(0, emailPage.getContent().size());
    }

    @Test
    void shouldReturnMailById() throws Exception {
        var email = createRandomEmail(1);

        MvcResult mvcResult = this.mockMvc.perform(get("/api/email/"+email.getId())).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        Email actualEmail = mapFromJson(mvcResult.getResponse().getContentAsString(), Email.class);
        assertEquals(actualEmail, email);
    }

    @Test
    void shouldReturnNotFoundCodeWhenMailIdIsNotValid() throws Exception {
        this.mockMvc.perform(get("/api/email/123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnAttachmentForEmail() throws Exception {
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
    void shouldReturnErrorWhenAttachmentIsRequestedButAttachmentIdIsNotValid() throws Exception {
        var email = createRandomEmail(1);

        this.mockMvc.perform(get("/api/email/"+email.getId()+"/attachment/123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnErrorWhenAttachmentIsRequestedButMailIdIsNotValid() throws Exception {
        var email = createRandomEmail(1);

        this.mockMvc.perform(get("/api/email/123/attachment/"+email.getAttachments().get(0).getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteEmail() throws Exception {
        var email = createRandomEmail(1);

        this.mockMvc.perform(delete("/api/email/"+email.getId()))
                .andExpect(status().is2xxSuccessful());

        assertThat(emailRepository.findAll(), empty());
    }

    @Test
    void shouldDeleteAllEmails() throws Exception {
        var email = createRandomEmails(5, 1);

        assertThat(emailRepository.findAll(), hasSize(5));

        this.mockMvc.perform(delete("/api/email"))
                .andExpect(status().is2xxSuccessful());

        assertThat(emailRepository.findAll(), empty());
    }

    private static <T> T mapFromJson(String json, Class<T> clazz) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, clazz);
    }

    private static <T> T mapFromJson(String json, TypeReference<T> typeReference) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, typeReference);
    }

    private List<Email> createRandomEmails(int numberOfEmails, int minusMinutes) {
        return IntStream.range(0, numberOfEmails).mapToObj(i -> createRandomEmail(minusMinutes)).collect(Collectors.toList());
    }

    private Email createRandomEmail(int minusMinutes) {
        return emailRepository.save(EmailControllerUtil.prepareRandomEmail(minusMinutes));
    }

}