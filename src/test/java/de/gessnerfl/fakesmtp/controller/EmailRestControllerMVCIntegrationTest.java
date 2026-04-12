package de.gessnerfl.fakesmtp.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailPartProcessingStatus;
import de.gessnerfl.fakesmtp.model.RestResponsePage;
import de.gessnerfl.fakesmtp.model.query.*;
import de.gessnerfl.fakesmtp.repository.EmailAttachmentRepository;
import de.gessnerfl.fakesmtp.repository.EmailContentRepository;
import de.gessnerfl.fakesmtp.repository.EmailInlineImageRepository;
import de.gessnerfl.fakesmtp.repository.EmailRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class EmailRestControllerMVCIntegrationTest {

    private static final String INVALID_INLINE_IMAGE_CONTENT_TYPE_MESSAGE = "INVALID_INLINE_IMAGE_CONTENT_TYPE: Stored inline image content type is invalid";
    private static final String INVALID_INLINE_IMAGE_BASE64_MESSAGE = "INVALID_INLINE_IMAGE_BASE64: Stored inline image data is invalid";

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private EmailAttachmentRepository emailAttachmentRepository;

    @Autowired
    private EmailContentRepository emailContentRepository;

    @Autowired
    private EmailInlineImageRepository emailInlineImageRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void init() {
        emailAttachmentRepository.deleteAllInBatch();
        emailContentRepository.deleteAllInBatch();
        emailInlineImageRepository.deleteAllInBatch();
        emailRepository.deleteAllInBatch();
    }

    @Test
    void shouldReturnEmptyListWhenNoEmailsAreAvailable() throws Exception {
        final var mvcResult = mockMvc.perform(get("/api/emails")).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        final var emailPage = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {});
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

        final var mvcResult = this.mockMvc.perform(get("/api/emails?page=0&size=2")).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        final var emailPage = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {});
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

        final var mvcResult = this.mockMvc.perform(get("/api/emails?page=1&size=2")).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        final var emailPage = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {});
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

        final var mvcResult = this.mockMvc.perform(get("/api/emails?page=2&size=1")).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        final var emailPage = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {});
        assertEquals(2, emailPage.getNumber());
        assertEquals(1, emailPage.getSize());
        assertEquals(1, emailPage.getTotalPages());
        assertEquals(1, emailPage.getTotalElements());
        assertEquals(0, emailPage.getContent().size());
    }

    @Test
    void shouldReturnMailById() throws Exception {
        var email = createRandomEmail(1);

        final var mvcResult = this.mockMvc.perform(get("/api/emails/" + email.getId())).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        final var actualEmail = mapFromJson(mvcResult.getResponse().getContentAsString());
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
        var attachment = email.getAttachments().getFirst();

        this.mockMvc.perform(get("/api/emails/" + email.getId() + "/attachments/" + attachment.getId()))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + attachment.getFilename()))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, String.valueOf(attachment.getData().length)))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().bytes(attachment.getData()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnInlineImageForEmail() throws Exception {
        var email = save(EmailControllerUtil.prepareEmailWithAllChildren(1));
        var inlineImage = email.getInlineImages().getFirst();
        var expectedImageData = Base64.getDecoder().decode(inlineImage.getData().getBytes(StandardCharsets.UTF_8));

        this.mockMvc.perform(get("/api/emails/" + email.getId() + "/inline-images/" + inlineImage.getId()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, startsWith(MediaType.IMAGE_PNG_VALUE)))
                .andExpect(content().bytes(expectedImageData))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"%%%invalid%%%base64%%%", "not-base64!"})
    void shouldReturn422WhenInlineImageDataIsInvalidBase64(String inlineImageData) throws Exception {
        var email = EmailControllerUtil.prepareEmailWithAllChildren(1);
        var inlineImage = email.getInlineImages().getFirst();
        inlineImage.setData(inlineImageData);
        var savedEmail = save(email);

        this.mockMvc.perform(get("/api/emails/" + savedEmail.getId() + "/inline-images/" + inlineImage.getId()))
                .andExpect(status().is(422))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, startsWith(MediaType.TEXT_PLAIN_VALUE)))
                .andExpect(content().string(INVALID_INLINE_IMAGE_BASE64_MESSAGE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-content-type", "text/", "/png"})
    void shouldReturn422WhenInlineImageContentTypeIsInvalid(String inlineImageContentType) throws Exception {
        var email = EmailControllerUtil.prepareEmailWithAllChildren(1);
        var inlineImage = email.getInlineImages().getFirst();
        inlineImage.setContentType(inlineImageContentType);
        var savedEmail = save(email);

        this.mockMvc.perform(get("/api/emails/" + savedEmail.getId() + "/inline-images/" + inlineImage.getId()))
                .andExpect(status().is(422))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, startsWith(MediaType.TEXT_PLAIN_VALUE)))
                .andExpect(content().string(INVALID_INLINE_IMAGE_CONTENT_TYPE_MESSAGE));
    }

    @Test
    void shouldRejectPersistingInlineImageWithNullData() {
        var email = EmailControllerUtil.prepareEmailWithAllChildren(1);
        email.getInlineImages().getFirst().setData(null);

        assertThrows(DataIntegrityViolationException.class, () -> emailRepository.saveAndFlush(email));
    }

    @Test
    void shouldRejectPersistingInlineImageWithNullContentType() {
        var email = EmailControllerUtil.prepareEmailWithAllChildren(1);
        email.getInlineImages().getFirst().setContentType(null);

        assertThrows(DataIntegrityViolationException.class, () -> emailRepository.saveAndFlush(email));
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

        this.mockMvc.perform(get("/api/emails/123/attachments/" + email.getAttachments().getFirst().getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn413WhenAttachmentWasSkippedDuringProcessing() throws Exception {
        var message = "SKIPPED_TOO_LARGE: Attachment exceeded configured max attachment size";
        var email = EmailControllerUtil.prepareRandomEmail(1);
        var attachment = email.getAttachments().getFirst();
        attachment.setProcessingStatus(EmailPartProcessingStatus.SKIPPED_TOO_LARGE);
        attachment.setProcessingMessage(message);
        var savedEmail = save(email);

        this.mockMvc.perform(get("/api/emails/" + savedEmail.getId() + "/attachments/" + attachment.getId()))
                .andExpect(status().is(413))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, startsWith(MediaType.TEXT_PLAIN_VALUE)))
                .andExpect(content().string(message));
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

        final var mvcResult = this.mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();

        final var emailSearchResult = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {});

        assertEquals(5, emailSearchResult.getNumberOfElements());
    }

    @Test
    void shouldSearchEmailsByToAddress() throws Exception {
        createRandomEmails(5, 1);
        var email1 = save(EmailControllerUtil.prepareEmail("subject", "an@address.domain", 1));

        final var mvcResult = this.mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filter\": {" +
                                "\"type\": \"equal\"," +
                                "\"property\": \"toAddress\"," +
                                "\"value\": \"an@address.domain\"" +
                                "}, " +
                                "\"sort\": {\"orders\": [" +
                                "{\"property\": \"receivedOn\", \"direction\": \"ASC\"}" +
                                "]}}"))
                .andReturn();

        final var emailSearchResult = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {});

        assertEquals(1, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByMessageId() throws Exception {
        final var messageId = "my-message-id";

        createRandomEmails(5, 1);
        final var email1 = save(EmailControllerUtil.prepareEmail("subject", "an@address.domain", 1, messageId));

        final var likeExpression = new LikeExpression("messageId", messageId);
        final var sorting = Sorting.by(new SortOrder("receivedOn"));
        final var searchRequest = SearchRequest.of(likeExpression, sorting);

        final var mvcResult = this.mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        final var emailSearchResult = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {});

        assertEquals(1, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByOtherMessageIds() throws Exception {
        final var messageId = "my-message-id";

        final var emails  = createRandomEmails(5, 1);
        save(EmailControllerUtil.prepareEmail("subject", "an@address.domain", 1, messageId));

        final var notEqualExpression = new NotEqualExpression("messageId", messageId);
        final var sorting = Sorting.by(new SortOrder("receivedOn"));
        final var searchRequest = SearchRequest.of(notEqualExpression, sorting);

        final var mvcResult = this.mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        final var emailSearchResult = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {});

        assertEquals(emails.size(), emailSearchResult.getNumberOfElements());
        assertEquals(emails, emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByToAddressContains() throws Exception {
        var email1 = save(EmailControllerUtil.prepareEmail("hello world", "hello@address.domain", 1));
        createRandomEmails(5, 1);
        var email2 = save(EmailControllerUtil.prepareEmail("subject", "an@address.domain", 1));

        final var mvcResult = this.mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filter\": {" +
                                "\"type\": \"like\"," +
                                "\"property\": \"toAddress\"," +
                                "\"value\": \"address.domain\"" +
                                "}, " +
                                "\"sort\": { " +
                                "\"orders\": [" +
                                "{\"property\": \"receivedOn\", \"direction\": \"ASC\"}" +
                                "]}}"))
                .andReturn();

        final var emailSearchResult = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {});

        assertEquals(2, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1, email2), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsWithEmptySortAndPagingData() throws Exception {
        var email1 = save(EmailControllerUtil.prepareEmail("hello world", "hello@address.domain", 1));
        createRandomEmails(5, 1);
        var email2 = save(EmailControllerUtil.prepareEmail("subject", "an@address.domain", 1));

        final var mvcResult = this.mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filter\": {" +
                                "\"type\": \"like\"," +
                                "\"property\": \"toAddress\"," +
                                "\"value\": \"address.domain\"" +
                                "}, " +
                                "\"sort\": {}," +
                                "\"page\": null," +
                                "\"size\": null" +
                                "}"))
                .andReturn();

        final var emailSearchResult = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {});

        assertEquals(2, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1, email2), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByToAddressContainsAndSubjectEqual() throws Exception {
        final var email1 = save(EmailControllerUtil.prepareEmail("hello world", "hello@address.domain", 1));
        createRandomEmails(5, 1);
        save(EmailControllerUtil.prepareEmail("subject", "an@address.domain", 1));
        save(EmailControllerUtil.prepareEmail("hello world", "an@something.domain", 1));

        final var expression = new LogicalAnd(Arrays.asList(
                new LikeExpression("toAddress", "address.domain"),
                new EqualExpression("subject", "hello world")
        ));
        final var sorting = Sorting.by(new SortOrder("receivedOn"));
        final var searchRequest = SearchRequest.of(expression, sorting);

        final var json = mapToJson(searchRequest);
        final var mvcResult = this.mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();

        final var emailSearchResult = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {});

        assertEquals(1, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByToAddressContainsOrSubjectEqual() throws Exception {
        var email1 = save(EmailControllerUtil.prepareEmail("hello world", "hello@address.domain", 1));
        createRandomEmails(5, 1);
        var email2 = save(EmailControllerUtil.prepareEmail("subject", "an@address.domain", 1));
        var email3 = save(EmailControllerUtil.prepareEmail("hello world", "an@something.domain", 1));

        final var expression = new LogicalOr(Arrays.asList(
                new LikeExpression("toAddress", "address.domain"),
                new EqualExpression("subject", "hello world")
        ));
        final var sorting = Sorting.by(new SortOrder("receivedOn"));
        final var searchRequest = SearchRequest.of(expression, sorting);

        final var mvcResult = this.mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        final var emailSearchResult = mapFromJson(mvcResult.getResponse().getContentAsString(), new TypeReference<RestResponsePage<Email>>() {});

        assertEquals(3, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1, email2, email3), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByReceivedOnGreaterThanOrEqualDates() throws Exception {
        final var now = getUtcNow();
        final var startDate = now.minusMinutes(1);

        createRandomEmails(5, 5);
        final var email1 = createRandomEmail(0);
        createRandomEmails(5, 10);
        createRandomEmails(5, 15);

        final var formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        final var greaterThanOrEqualExpression = new GreaterThanOrEqualExpression("receivedOn", startDate.format(formatter));
        final var searchRequest = SearchRequest.of(greaterThanOrEqualExpression);

        final var mvcResult = mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        final var responseContent = mvcResult.getResponse().getContentAsString();
        final var emailSearchResult = mapFromJson(responseContent, new TypeReference<RestResponsePage<Email>>() {});

        assertEquals(1, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByReceivedOnGreaterThanDates() throws Exception {
        final var now = getUtcNow();
        final var startDate = now.minusMinutes(1);

        createRandomEmails(5, 5);
        final var email1 = createRandomEmail(0);
        createRandomEmails(5, 10);
        createRandomEmails(5, 15);

        final var formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        final var greaterThanExpression = new GreaterThanExpression("receivedOn", startDate.format(formatter));
        final var searchRequest = SearchRequest.of(greaterThanExpression);

        final var mvcResult = mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        final var responseContent = mvcResult.getResponse().getContentAsString();
        final var emailSearchResult = mapFromJson(responseContent, new TypeReference<RestResponsePage<Email>>() {});

        assertEquals(1, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByReceivedOnLessThanDates() throws Exception {
        final var now = getUtcNow();
        final var endDate = now.minusMinutes(20);

        createRandomEmails(5, 5);
        createRandomEmails(5, 10);
        final var email1 = createRandomEmail(25);
        createRandomEmails(5, 15);

        final var formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        final var lessThanExpression = new LessThanExpression("receivedOn", endDate.format(formatter));
        final var searchRequest = SearchRequest.of(lessThanExpression);

        final var mvcResult = mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        final var responseContent = mvcResult.getResponse().getContentAsString();
        final var emailSearchResult = mapFromJson(responseContent, new TypeReference<RestResponsePage<Email>>() {
        });

        assertEquals(1, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1), emailSearchResult.getContent());
    }

    private static ZonedDateTime getUtcNow() {
        return ZonedDateTime.now(ZoneId.of("UTC"));
    }

    @Test
    void shouldSearchEmailsByReceivedOnLessThanOrEqualDates() throws Exception {
        final var now = getUtcNow();
        final var endDate = now.minusMinutes(20);

        createRandomEmails(5, 5);
        createRandomEmails(5, 10);
        final var email1 = createRandomEmail(25);
        createRandomEmails(5, 15);

        final var formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        final var lessThanOrEqualExpression = new LessThanOrEqualExpression("receivedOn", endDate.format(formatter));
        final var searchRequest = SearchRequest.of(lessThanOrEqualExpression);

        final var mvcResult = mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        final var responseContent = mvcResult.getResponse().getContentAsString();
        final var emailSearchResult = mapFromJson(responseContent, new TypeReference<RestResponsePage<Email>>() {});

        assertEquals(1, emailSearchResult.getNumberOfElements());
        assertEquals(List.of(email1), emailSearchResult.getContent());
    }

    @Test
    void shouldSearchEmailsByMessageIdNull() throws Exception {
        createRandomEmails(5, 5);

        final var isNull = new IsNullExpression("messageId");
        final var searchRequest = SearchRequest.of(isNull);

        final var mvcResult = mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        final var responseContent = mvcResult.getResponse().getContentAsString();
        final var emailSearchResult = mapFromJson(responseContent, new TypeReference<RestResponsePage<Email>>() {});

        assertEquals(0, emailSearchResult.getNumberOfElements());
    }

    @Test
    void shouldSearchEmailsByMessageIdNotNull() throws Exception {
        int numberOfEmails = 5;
        createRandomEmails(numberOfEmails, 5);

        final var isNotNull = new IsNotNullExpression("messageId");
        final var searchRequest = SearchRequest.of(isNotNull);

        final var mvcResult = mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        final var responseContent = mvcResult.getResponse().getContentAsString();
        final var emailSearchResult = mapFromJson(responseContent, new TypeReference<RestResponsePage<Email>>() {});

        assertEquals(numberOfEmails, emailSearchResult.getNumberOfElements());
    }

    @Test
    void shouldSearchEmailsByNegatedMessageIdNotNull() throws Exception {
        int numberOfEmails = 5;
        createRandomEmails(numberOfEmails, 5);

        final var isNotNull = new IsNotNullExpression("messageId");
        final var negation = new Negation(isNotNull);
        final var searchRequest = SearchRequest.of(negation);

        final var mvcResult = mockMvc.perform(post("/api/emails/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapToJson(searchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        final var responseContent = mvcResult.getResponse().getContentAsString();
        final var emailSearchResult = mapFromJson(responseContent, new TypeReference<RestResponsePage<Email>>() {});

        assertEquals(0, emailSearchResult.getNumberOfElements());
    }

    @Test
    void shouldDeleteAllEmailsWithAttachmentsContentAndInlineImages() throws Exception {
        save(EmailControllerUtil.prepareEmailWithAllChildren(1));
        save(EmailControllerUtil.prepareEmailWithAllChildren(2));
        save(EmailControllerUtil.prepareEmailWithAllChildren(3));

        assertThat(emailRepository.findAll(), hasSize(3));
        assertThat(emailAttachmentRepository.findAll(), hasSize(3));
        assertThat(emailContentRepository.findAll(), hasSize(3));
        assertThat(emailInlineImageRepository.findAll(), hasSize(3));

        this.mockMvc.perform(delete("/api/emails"))
                .andExpect(status().is2xxSuccessful());

        assertThat(emailRepository.findAll(), empty());
        assertThat(emailAttachmentRepository.findAll(), empty());
        assertThat(emailContentRepository.findAll(), empty());
        assertThat(emailInlineImageRepository.findAll(), empty());
    }

    @Test
    void shouldDeleteSingleEmailWithCascade() throws Exception {
        var email = save(EmailControllerUtil.prepareEmailWithAllChildren(1));
        var emailId = email.getId();

        assertThat(emailRepository.findAll(), hasSize(1));
        assertThat(emailAttachmentRepository.findAll(), hasSize(1));
        assertThat(emailContentRepository.findAll(), hasSize(1));
        assertThat(emailInlineImageRepository.findAll(), hasSize(1));

        this.mockMvc.perform(delete("/api/emails/" + emailId))
                .andExpect(status().is2xxSuccessful());

        assertThat(emailRepository.findAll(), empty());
        assertThat(emailAttachmentRepository.findAll(), empty());
        assertThat(emailContentRepository.findAll(), empty());
        assertThat(emailInlineImageRepository.findAll(), empty());
    }

    private Email mapFromJson(String json) throws IOException {
        return objectMapper.readValue(json, Email.class);
    }

    private <T> T mapFromJson(String json, TypeReference<T> typeReference) throws IOException {
        return objectMapper.readValue(json, typeReference);
    }

    private List<Email> createRandomEmails(int numberOfEmails, int minusMinutes) {
        return IntStream.range(0, numberOfEmails).mapToObj(_ -> createRandomEmail(minusMinutes)).collect(Collectors.toList());
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
