package de.gessnerfl.fakesmtp.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.RestResponsePage;
import de.gessnerfl.fakesmtp.model.query.*;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("mockserver")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class EmailRestControllerMVCIntegrationTest {

    @Autowired
    private EmailRepository emailRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void init() {
        emailRepository.deleteAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoEmailsAreAvailable() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/emails")).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        RestResponsePage<Email> emailPage = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {
        });
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
        RestResponsePage<Email> emailPage = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {
        });
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
        RestResponsePage<Email> emailPage = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {
        });
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
        RestResponsePage<Email> emailPage = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {
        });
        assertEquals(2, emailPage.getNumber());
        assertEquals(1, emailPage.getSize());
        assertEquals(1, emailPage.getTotalPages());
        assertEquals(1, emailPage.getTotalElements());
        assertEquals(0, emailPage.getContent().size());
    }

    @Test
    void shouldReturnMailById() throws Exception {
        var email = createRandomEmail(1);

        MvcResult mvcResult = this.mockMvc.perform(get("/api/emails/" + email.getId())).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        Email actualEmail = mapFromJson(mvcResult.getResponse().getContentAsString());
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

        this.mockMvc.perform(get("/api/emails/" + email.getId() + "/attachments/" + attachment.getId()))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + attachment.getFilename()))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, String.valueOf(attachment.getData().length)))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().bytes(attachment.getData()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnErrorWhenAttachmentIsRequestedButAttachmentIdIsNotValid() throws Exception {
        var email = createRandomEmail(1);

        this.mockMvc.perform(get("/api/emails/" + email.getId() + "/attachments/123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnErrorWhenAttachmentIsRequestedButMailIdIsNotValid() throws Exception {
        var email = createRandomEmail(1);

        this.mockMvc.perform(get("/api/emails/123/attachments/" + email.getAttachments().get(0).getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteEmail() throws Exception {
        var email = createRandomEmail(1);

        this.mockMvc.perform(delete("/api/emails/" + email.getId()))
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
    void shouldSearchEmailsByEmptyCriterias() throws Exception {
        createRandomEmails(5, 1);

        MvcResult mvcResult = this.mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();

        RestResponsePage<Email> emailSearchResult = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {
        });

        assertEquals(emailSearchResult.getNumberOfElements(), 5);
    }

    @Test
    void shouldSearchEmailsByToAddress() throws Exception {
        createRandomEmails(5, 1);
        var email1 = save(EmailControllerUtil.prepareEmail("subject", "an@address.domain", 1));

        MvcResult mvcResult = this.mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filter\": {" +
                                "\"type\": \"biexp\"," +
                                "\"property\": \"toAddress\"," +
                                "\"operator\": \"EQUAL\"," +
                                "\"value\": \"an@address.domain\"" +
                                "}, " +
                                "\"sort\": {\"orders\": [" +
                                "{\"property\": \"receivedOn\", \"direction\": \"ASC\"}" +
                                "]}}"))
                .andReturn();

        RestResponsePage<Email> emailSearchResult = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {
        });

        assertEquals(1, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByMessageId() throws Exception {
        final var messageId = "my-message-id";

        createRandomEmails(5, 1);
        final var email1 = save(EmailControllerUtil.prepareEmail("subject", "an@address.domain", 1, messageId));

        final var binaryExpression = new BinaryExpression("messageId", BinaryOperator.LIKE, messageId);
        final var sorting = Sorting.by(new SortOrder("receivedOn"));
        final var searchRequest = SearchRequest.of(binaryExpression, sorting);

        MvcResult mvcResult = this.mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        RestResponsePage<Email> emailSearchResult = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {
        });

        assertEquals(emailSearchResult.getNumberOfElements(), 1);
        assertEquals(List.of(email1), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByOtherMessageIds() throws Exception {
        final var messageId = "my-message-id";

        final var emails  = createRandomEmails(5, 1);
        final var email1 = save(EmailControllerUtil.prepareEmail("subject", "an@address.domain", 1, messageId));

        final var binaryExpression = new BinaryExpression("messageId", BinaryOperator.NOT_EQUAL, messageId);
        final var sorting = Sorting.by(new SortOrder("receivedOn"));
        final var searchRequest = SearchRequest.of(binaryExpression, sorting);

        MvcResult mvcResult = this.mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        RestResponsePage<Email> emailSearchResult = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {
        });

        assertEquals(emails.size(), emailSearchResult.getNumberOfElements());
        assertEquals(emails, emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByToAddressContains() throws Exception {
        var email1 = save(EmailControllerUtil.prepareEmail("hello world", "hello@address.domain", 1));
        createRandomEmails(5, 1);
        var email2 = save(EmailControllerUtil.prepareEmail("subject", "an@address.domain", 1));

        MvcResult mvcResult = this.mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filter\": {" +
                                "\"type\": \"biexp\"," +
                                "\"property\": \"toAddress\"," +
                                "\"operator\": \"LIKE\"," +
                                "\"value\": \"address.domain\"" +
                                "}, " +
                                "\"sort\": { " +
                                "\"orders\": [" +
                                "{\"property\": \"receivedOn\", \"direction\": \"ASC\"}" +
                                "]}}"))
                .andReturn();

        RestResponsePage<Email> emailSearchResult = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {
        });

        assertEquals(2, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1, email2), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsWithEmptySortAndPagingData() throws Exception {
        var email1 = save(EmailControllerUtil.prepareEmail("hello world", "hello@address.domain", 1));
        createRandomEmails(5, 1);
        var email2 = save(EmailControllerUtil.prepareEmail("subject", "an@address.domain", 1));

        MvcResult mvcResult = this.mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filter\": {" +
                                "\"type\": \"biexp\"," +
                                "\"property\": \"toAddress\"," +
                                "\"operator\": \"LIKE\"," +
                                "\"value\": \"address.domain\"" +
                                "}, " +
                                "\"sort\": {}," +
                                "\"page\": null," +
                                "\"size\": null" +
                                "}"))
                .andReturn();

        RestResponsePage<Email> emailSearchResult = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {
        });

        assertEquals(2, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1, email2), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByToAddressContainsAndSubjectEqual() throws Exception {
        final var email1 = save(EmailControllerUtil.prepareEmail("hello world", "hello@address.domain", 1));
        createRandomEmails(5, 1);
        save(EmailControllerUtil.prepareEmail("subject", "an@address.domain", 1));
        save(EmailControllerUtil.prepareEmail("hello world", "an@something.domain", 1));

        final var expression = LogicalAnd.of(
                new BinaryExpression("toAddress", BinaryOperator.LIKE, "address.domain"),
                new BinaryExpression("subject", BinaryOperator.EQUAL, "hello world")
        );
        final var sorting = Sorting.by(new SortOrder("receivedOn"));
        final var searchRequest = SearchRequest.of(expression, sorting);

        final var json = mapToJson(searchRequest);
        MvcResult mvcResult = this.mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();

        RestResponsePage<Email> emailSearchResult = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {
        });

        assertEquals(1, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByToAddressContainsOrSubjectEqual() throws Exception {
        var email1 = save(EmailControllerUtil.prepareEmail("hello world", "hello@address.domain", 1));
        createRandomEmails(5, 1);
        var email2 = save(EmailControllerUtil.prepareEmail("subject", "an@address.domain", 1));
        var email3 = save(EmailControllerUtil.prepareEmail("hello world", "an@something.domain", 1));

        final var expression = LogicalOr.of(
                new BinaryExpression("toAddress", BinaryOperator.LIKE, "address.domain"),
                new BinaryExpression("subject", BinaryOperator.EQUAL, "hello world")
        );
        final var sorting = Sorting.by(new SortOrder("receivedOn"));
        final var searchRequest = SearchRequest.of(expression, sorting);

        MvcResult mvcResult = this.mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        RestResponsePage<Email> emailSearchResult = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {
        });

        assertEquals(3, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1, email2, email3), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByReceivedOnGreaterThanOrEqualDates() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusMinutes(1);

        createRandomEmails(5, 5);
        Email email1 = createRandomEmail(0);
        createRandomEmails(5, 10);
        createRandomEmails(5, 15);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        final var binaryExpression = new BinaryExpression("receivedOn", BinaryOperator.GREATER_THAN_OR_EQUAL, startDate.format(formatter));
        final var searchRequest = SearchRequest.of(binaryExpression);

        MvcResult mvcResult = mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        RestResponsePage<Email> emailSearchResult = mapFromJson(responseContent, new TypeReference<RestResponsePage<Email>>() {
        });

        assertEquals(1, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByReceivedOnGreaterThanDates() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusMinutes(1);

        createRandomEmails(5, 5);
        Email email1 = createRandomEmail(0);
        createRandomEmails(5, 10);
        createRandomEmails(5, 15);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        final var binaryExpression = new BinaryExpression("receivedOn", BinaryOperator.GREATER_THAN, startDate.format(formatter));
        final var searchRequest = SearchRequest.of(binaryExpression);

        MvcResult mvcResult = mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        RestResponsePage<Email> emailSearchResult = mapFromJson(responseContent, new TypeReference<RestResponsePage<Email>>() {
        });

        assertEquals(1, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByReceivedOnLessThanDates() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.minusMinutes(20);

        createRandomEmails(5, 5);
        createRandomEmails(5, 10);
        Email email1 = createRandomEmail(25);
        createRandomEmails(5, 15);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        final var binaryExpression = new BinaryExpression("receivedOn", BinaryOperator.LESS_THAN, endDate.format(formatter));
        final var searchRequest = SearchRequest.of(binaryExpression);

        MvcResult mvcResult = mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        RestResponsePage<Email> emailSearchResult = mapFromJson(responseContent, new TypeReference<RestResponsePage<Email>>() {
        });

        assertEquals(1, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByReceivedOnLessThanOrEqualDates() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.minusMinutes(20);

        createRandomEmails(5, 5);
        createRandomEmails(5, 10);
        Email email1 = createRandomEmail(25);
        createRandomEmails(5, 15);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        final var binaryExpression = new BinaryExpression("receivedOn", BinaryOperator.LESS_THAN_OR_EQUAL, endDate.format(formatter));
        final var searchRequest = SearchRequest.of(binaryExpression);

        MvcResult mvcResult = mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        RestResponsePage<Email> emailSearchResult = mapFromJson(responseContent, new TypeReference<RestResponsePage<Email>>() {
        });

        assertEquals(1, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByMessageIdNull() throws Exception {
        createRandomEmails(5, 5);

        final var unaryExpression = new UnaryExpression("messageId", UnaryOperator.IS_NULL);
        final var searchRequest = SearchRequest.of(unaryExpression);

        MvcResult mvcResult = mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        RestResponsePage<Email> emailSearchResult = mapFromJson(responseContent, new TypeReference<RestResponsePage<Email>>() {
        });

        assertEquals(0, emailSearchResult.getNumberOfElements());
    }

    @Test
    void shouldSearchEmailsByMessageIdNotNull() throws Exception {
        int numberOfEmails = 5;
        createRandomEmails(numberOfEmails, 5);

        final var unaryExpression = new UnaryExpression("messageId", UnaryOperator.NOT_NULL);
        final var searchRequest = SearchRequest.of(unaryExpression);

        MvcResult mvcResult = mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        RestResponsePage<Email> emailSearchResult = mapFromJson(responseContent, new TypeReference<RestResponsePage<Email>>() {
        });

        assertEquals(numberOfEmails, emailSearchResult.getNumberOfElements());
    }

    @Test
    void shouldSearchEmailsByNegatedMessageIdNotNull() throws Exception {
        int numberOfEmails = 5;
        createRandomEmails(numberOfEmails, 5);

        final var unaryExpression = new UnaryExpression("messageId", UnaryOperator.NOT_NULL);
        final var negation = new Negation(unaryExpression);
        final var searchRequest = SearchRequest.of(negation);

        MvcResult mvcResult = mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        RestResponsePage<Email> emailSearchResult = mapFromJson(responseContent, new TypeReference<RestResponsePage<Email>>() {
        });

        assertEquals(0, emailSearchResult.getNumberOfElements());
    }


    private Email mapFromJson(String json) throws IOException {
        return objectMapper.readValue(json, Email.class);
    }

    private <T> T mapFromJson(String json, TypeReference<T> typeReference) throws IOException {
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

    public String mapToJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

}