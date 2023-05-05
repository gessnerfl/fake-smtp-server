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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("mockserver")
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
        MvcResult mvcResult = mockMvc.perform(get("/api/emails")).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        RestResponsePage<Email> emailPage = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {});
        assertEquals(0, emailPage.getNumber());
        assertEquals(0, emailPage.getNumberOfElements());
        assertEquals(0, emailPage.getTotalElements());
        assertEquals(0, emailPage.getContent().size());
    }

    @Test
    void shouldReturnFirstPageOfEmails() throws Exception {
        createRandomEmail(5);
        var email2 = createRandomEmail(2);
        var email3 = createRandomEmail(1);

        MvcResult mvcResult = this.mockMvc.perform(get("/api/emails?page=0&size=2")).andReturn();

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
        createRandomEmail(2);
        createRandomEmail(1);

        MvcResult mvcResult = this.mockMvc.perform(get("/api/emails?page=1&size=2")).andReturn();

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
        createRandomEmail(5);

        MvcResult mvcResult = this.mockMvc.perform(get("/api/emails?page=2&size=1")).andReturn();

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

        MvcResult mvcResult = this.mockMvc.perform(get("/api/emails/"+email.getId())).andReturn();

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

        this.mockMvc.perform(get("/api/emails/"+email.getId()+"/attachments/" + attachment.getId()))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,"attachment;filename=" + attachment.getFilename()))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH,"" + attachment.getData().length))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().bytes(attachment.getData()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnErrorWhenAttachmentIsRequestedButAttachmentIdIsNotValid() throws Exception {
        var email = createRandomEmail(1);

        this.mockMvc.perform(get("/api/emails/"+email.getId()+"/attachments/123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnErrorWhenAttachmentIsRequestedButMailIdIsNotValid() throws Exception {
        var email = createRandomEmail(1);

        this.mockMvc.perform(get("/api/emails/123/attachments/"+email.getAttachments().get(0).getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteEmail() throws Exception {
        var email = createRandomEmail(1);

        this.mockMvc.perform(delete("/api/emails/"+email.getId()))
                .andExpect(status().is2xxSuccessful());

        assertThat(emailRepository.findAll(), empty());
    }

    @Test
    void shouldDeleteAllEmails() throws Exception {
        createRandomEmails(5, 1);

        assertThat(emailRepository.findAll(), hasSize(5));

        this.mockMvc.perform(delete("/api/emails"))
                .andExpect(status().is2xxSuccessful());

        assertThat(emailRepository.findAll(), empty());
    }

    @Test
    void shouldSearchEmailsByToAddress() throws Exception {
        createRandomEmails(5, 1);

        MvcResult mvcResult = this.mockMvc.perform(get("/api/emails/search?query=toAddress:receiver@example.com"))
                .andReturn();

        @SuppressWarnings("unchecked")
        ArrayList<Email> emailSearch = mapFromJson(mvcResult.getResponse().getContentAsString(), ArrayList.class);

        assertEquals(emailSearch.size(), 5);
    }

    @Test
    void shouldSearchEmailsById() throws Exception {
        createRandomEmails(5, 1);
        var email = createRandomEmail(5);

        MvcResult mvcResult = this.mockMvc.perform(get("/api/emails/search?query=id:" + email.getId()))
                .andReturn();

        ArrayList<Email> emailSearch = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<ArrayList<Email>>() {});

        assertEquals(emailSearch.size(), 1);
        assertEquals(email.getId(), emailSearch.get(0).getId());
        assertEquals(email.getFromAddress(), emailSearch.get(0).getFromAddress());
        assertEquals(email.getMessageId(), emailSearch.get(0).getMessageId());
        assertEquals(email.getSubject(), emailSearch.get(0).getSubject());
        assertEquals(email.getToAddress(), emailSearch.get(0).getToAddress());
    }

    @Test
    void shouldSearchEmailsByToAddressRegex() throws Exception {
        createRandomEmails(5, 1);

        MvcResult mvcResult = this.mockMvc.perform(get("/api/emails/search?query=toAddress:*receiver*"))
                .andReturn();

        ArrayList<Email> emailSearch = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<ArrayList<Email>>() {});

        assertEquals(emailSearch.size(), 5);
    }

    @Test
    void shouldSearchEmailsBySubjectRegexAndToAddress() throws Exception {
        createRandomEmails(5, 1);
        var email1 = save(EmailControllerUtil.prepareEmail("subject", "friend@address.domain", 1));
        var email2 = save(EmailControllerUtil.prepareEmail("an another subject", "friend@address.domain", 15));

        MvcResult mvcResult = this.mockMvc.perform(get("/api/emails/search?query=subject:*subject*,AND,toAddress:friend@address.domain"))
                .andReturn();

        ArrayList<Email> emailSearch = mapFromJson(mvcResult.getResponse().getContentAsString(),new TypeReference<ArrayList<Email>>() {});
        
        assertEquals(emailSearch.size(), 2);
        assertEquals(email1.getId(), emailSearch.get(0).getId());
        assertEquals(email1.getFromAddress(), emailSearch.get(0).getFromAddress());
        assertEquals(email1.getMessageId(), emailSearch.get(0).getMessageId());
        assertEquals(email1.getSubject(), emailSearch.get(0).getSubject());
        assertEquals(email1.getToAddress(), emailSearch.get(0).getToAddress());

        assertEquals(email2.getId(), emailSearch.get(1).getId());
        assertEquals(email2.getFromAddress(), emailSearch.get(1).getFromAddress());
        assertEquals(email2.getMessageId(), emailSearch.get(1).getMessageId());
        assertEquals(email2.getSubject(), emailSearch.get(1).getSubject());
        assertEquals(email2.getToAddress(), emailSearch.get(1).getToAddress());
    }

    @Test
    void shouldSearchEmailsByToSubjectRegexOrToAddress() throws Exception {
        createRandomEmails(5, 1);
        save(EmailControllerUtil.prepareEmail("Subject", "friend@address.domain", 1));
        save(EmailControllerUtil.prepareEmail("an another Subject", "friend@address.domain", 15));

        MvcResult mvcResult = this.mockMvc.perform(get("/api/emails/search?query=subject:*Subject* OR toAddress:friend@address.domain"))
                .andReturn();

        ArrayList<Email> emailSearch = mapFromJson(mvcResult.getResponse().getContentAsString(),new TypeReference<ArrayList<Email>>() {});
        
        assertEquals(emailSearch.size(), 7);
    }

    @Test
    void shouldSearchEmailsByReceiveOn() throws Exception {
        createRandomEmails(5, 1);

        MvcResult mvcResult = this.mockMvc.perform(get("/api/emails/search?query=receivedOn:*02/05/2023*"))
                .andReturn();

        ArrayList<Email> emailSearch = mapFromJson(mvcResult.getResponse().getContentAsString(),new TypeReference<ArrayList<Email>>() {});
        
        assertEquals(emailSearch.size(), 5);
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
        return save(EmailControllerUtil.prepareRandomEmail(minusMinutes));
    }

    private Email save(Email email) {
        return emailRepository.save(email);
    }

}