package de.gessnerfl.fakesmtp.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import de.gessnerfl.fakesmtp.repository.EmailInlineImageRepository;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import jakarta.servlet.ServletException;

@ActiveProfiles("mockserver")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class EmailRestControllerTransactionalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailInlineImageRepository emailInlineImageRepository;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM email_attachment");
        jdbcTemplate.update("DELETE FROM email_content");
        jdbcTemplate.update("DELETE FROM email_inline_image");
        jdbcTemplate.update("DELETE FROM email");
    }

    @Test
    void shouldRollbackDeleteAllEmailsWhenInlineImageDeleteFails() throws Exception {
        emailRepository.save(EmailControllerUtil.prepareEmailWithAllChildren(1));
        emailRepository.save(EmailControllerUtil.prepareEmailWithAllChildren(2));

        var emailCountBefore = countRows("email");
        var attachmentCountBefore = countRows("email_attachment");
        var contentCountBefore = countRows("email_content");
        var inlineImageCountBefore = countRows("email_inline_image");

        doThrow(new RuntimeException("forced failure during inline image cleanup"))
                .when(emailInlineImageRepository)
                .deleteAllInBatch();

        assertThrows(ServletException.class, () -> this.mockMvc.perform(delete("/api/emails")));

        assertThat(countRows("email"), is(emailCountBefore));
        assertThat(countRows("email_attachment"), is(attachmentCountBefore));
        assertThat(countRows("email_content"), is(contentCountBefore));
        assertThat(countRows("email_inline_image"), is(inlineImageCountBefore));
    }

    private int countRows(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }
}
