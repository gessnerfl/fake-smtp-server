package de.gessnerfl.fakesmtp.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import de.gessnerfl.fakesmtp.config.WebappAuthenticationProperties;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailPartProcessingStatus;
import de.gessnerfl.fakesmtp.model.query.SearchRequest;
import de.gessnerfl.fakesmtp.model.query.SearchSpecification;
import de.gessnerfl.fakesmtp.repository.EmailAttachmentRepository;
import de.gessnerfl.fakesmtp.repository.EmailContentRepository;
import de.gessnerfl.fakesmtp.repository.EmailInlineImageRepository;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import de.gessnerfl.fakesmtp.service.EmailDeletionService;
import de.gessnerfl.fakesmtp.service.EmailSseEmitterService;
import de.gessnerfl.fakesmtp.util.MediaTypeUtil;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.servlet.ServletContext;

@RestController
@RequestMapping("/api/emails")
@Validated
public class EmailRestController {

    private static final Logger logger = LoggerFactory.getLogger(EmailRestController.class);
    private static final String DEFAULT_SORT_PROPERTY = "receivedOn";
    private static final String INVALID_INLINE_IMAGE_CONTENT_TYPE_MESSAGE = "INVALID_INLINE_IMAGE_CONTENT_TYPE: Stored inline image content type is invalid";
    private static final String INVALID_INLINE_IMAGE_BASE64_MESSAGE = "INVALID_INLINE_IMAGE_BASE64: Stored inline image data is invalid";

    private final EmailRepository emailRepository;
    private final EmailAttachmentRepository emailAttachmentRepository;
    private final EmailContentRepository emailContentRepository;
    private final EmailInlineImageRepository emailInlineImageRepository;
    private final MediaTypeUtil mediaTypeUtil;
    private final ServletContext servletContext;
    private final EmailDeletionService emailDeletionService;
    private final EmailSseEmitterService emailSseEmitterService;
    private final WebappAuthenticationProperties authProperties;

    @Autowired
    public EmailRestController(EmailRepository emailRepository,
            EmailAttachmentRepository emailAttachmentRepository,
            EmailContentRepository emailContentRepository,
            EmailInlineImageRepository emailInlineImageRepository,
            MediaTypeUtil mediaTypeUtil,
            ServletContext servletContext,
            EmailDeletionService emailDeletionService,
            EmailSseEmitterService emailSseEmitterService,
            WebappAuthenticationProperties authProperties) {
        this.emailRepository = emailRepository;
        this.emailAttachmentRepository = emailAttachmentRepository;
        this.emailContentRepository = emailContentRepository;
        this.emailInlineImageRepository = emailInlineImageRepository;
        this.mediaTypeUtil = mediaTypeUtil;
        this.servletContext = servletContext;
        this.emailDeletionService = emailDeletionService;
        this.emailSseEmitterService = emailSseEmitterService;
        this.authProperties = authProperties;
    }

    @GetMapping()
    @Parameter(name = "page", description = "Page number", example = "0")
    @Parameter(name = "size", description = "Page size", example = "1")
    @Parameter(name = "sort", description = "Sort criteria", example = DEFAULT_SORT_PROPERTY)
    public Page<Email> all(
            @SortDefault(sort = DEFAULT_SORT_PROPERTY, direction = Sort.Direction.DESC) @Parameter(hidden = true) Pageable pageable) {
        return emailRepository.findAll(pageable);
    }

    @GetMapping("/{id}")
    public Email getEmailById(@PathVariable Long id) {
        return emailRepository.findById(id).orElseThrow(() -> new EmailNotFoundException("Could not find email " + id));
    }

    @GetMapping("/{mailId}/attachments/{attachmentId}")
    public ResponseEntity<ByteArrayResource> getEmailAttachmentById(@PathVariable Long mailId,
            @PathVariable Long attachmentId) {
        var attachment = emailAttachmentRepository.findById(attachmentId)
                .filter(a -> a.getEmail().getId().equals(mailId))
                .orElseThrow(() -> new AttachmentNotFoundException(
                        "Attachment with id " + attachmentId + " not found for mail " + mailId));

        var mediaType = mediaTypeUtil.getMediaTypeForFileName(this.servletContext, attachment.getFilename());

        if (attachment.getProcessingStatus() == EmailPartProcessingStatus.SKIPPED_TOO_LARGE) {
            var message = attachment.getProcessingMessage() != null
                    ? attachment.getProcessingMessage()
                    : "SKIPPED_TOO_LARGE: Attachment was skipped during email processing";
            return ResponseEntity.status(413)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(new ByteArrayResource(message.getBytes(StandardCharsets.UTF_8)));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + attachment.getFilename())
                .contentType(mediaType)
                .contentLength(attachment.getData().length) //
                .body(new ByteArrayResource(attachment.getData()));
    }

    @GetMapping("/{mailId}/inline-images/{inlineImageId}")
    public ResponseEntity<ByteArrayResource> getEmailInlineImageById(@PathVariable Long mailId,
            @PathVariable Long inlineImageId) {
        var inlineImage = emailInlineImageRepository.findById(inlineImageId)
                .filter(i -> i.getEmail().getId().equals(mailId))
                .orElseThrow(() -> new InlineImageNotFoundException(
                        "Inline image with id " + inlineImageId + " not found for mail " + mailId));

        if (inlineImage.getProcessingStatus() == EmailPartProcessingStatus.SKIPPED_TOO_LARGE) {
            var message = inlineImage.getProcessingMessage() != null
                    ? inlineImage.getProcessingMessage()
                    : "SKIPPED_TOO_LARGE: Inline image was skipped during email processing";
            return ResponseEntity.status(413)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(new ByteArrayResource(message.getBytes(StandardCharsets.UTF_8)));
        }

        var mediaType = parseInlineImageMediaType(inlineImage.getContentType(), mailId, inlineImageId);
        if (mediaType == null) {
            return unprocessableInlineImageResponse(INVALID_INLINE_IMAGE_CONTENT_TYPE_MESSAGE);
        }

        var imageData = decodeInlineImageData(inlineImage.getData(), mailId, inlineImageId);
        if (imageData == null) {
            return unprocessableInlineImageResponse(INVALID_INLINE_IMAGE_BASE64_MESSAGE);
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(new ByteArrayResource(imageData));
    }

    private MediaType parseInlineImageMediaType(String contentType, Long mailId, Long inlineImageId) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException ex) {
            logger.warn("Inline image {} for mail {} has invalid content type", inlineImageId, mailId);
            return null;
        }
    }

    private byte[] decodeInlineImageData(String data, Long mailId, Long inlineImageId) {
        if (data == null) {
            logger.warn("Inline image {} for mail {} has no data", inlineImageId, mailId);
            return null;
        }

        try {
            return Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException ex) {
            logger.warn("Inline image {} for mail {} contains invalid Base64 data", inlineImageId, mailId);
            return null;
        }
    }

    private ResponseEntity<ByteArrayResource> unprocessableInlineImageResponse(String message) {
        return ResponseEntity.status(422)
                .contentType(MediaType.TEXT_PLAIN)
                .body(new ByteArrayResource(message.getBytes(StandardCharsets.UTF_8)));
    }

    @DeleteMapping("/{id}")
    public void deleteEmailById(@PathVariable Long id) {
        emailDeletionService.deleteEmailById(id);
    }

    @DeleteMapping()
    public void deleteAllEmails() {
        emailDeletionService.deleteAllEmails();
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToEmailEvents(Principal principal) {
        // If authentication is enabled, require authentication for SSE
        // If authentication is disabled, allow anonymous SSE connections
        if (authProperties.isAuthenticationEnabled()) {
            if (principal == null) {
                final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
                    throw new SecurityException("Authentication required for SSE connection");
                }
            }
        }
        
        final var emitter = emailSseEmitterService.createAndAddEmitter();

        try {
            emitter.send(SseEmitter.event()
                    .name("connection-established")
                    .data("Connected to email events"));
            logger.debug("SSE connection established for client");
        } catch (IOException e) {
            emitter.complete();
        }

        return emitter;
    }

    @PostMapping(value = "/search")
    public Page<Email> search(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "Search request", content = @Content(mediaType = "application/json", examples = {
                    @ExampleObject(value = """
                            {
                              "filter": {
                                  "type": "biexp",
                                  "property": "toAddress",
                                  "operator": "EQUAL",
                                  "value": "test@example.com"
                              },
                              "sort": {
                                  "orders": [
                                     {
                                        "property": "receivedOn",
                                        "direction": "DESC"
                                     }
                                  ]
                              },
                              "page": 0,
                              "size": 10
                            }"""),
                    @ExampleObject(value = """
                            {
                              "filter": {
                                  "type": "and",
                                  "expressions": [
                                      {
                                          "type": "biexp",
                                          "property": "toAddress",
                                          "operator": "EQUAL",
                                          "value": "test@example.com"
                                      },
                                      {
                                          "type": "biexp",
                                          "property": "subject",
                                          "operator": "LIKE",
                                          "value": "foo"
                                      }
                                  ]
                              },
                              "sort": {
                                  "orders": [
                                     {
                                        "property": "receivedOn",
                                        "direction": "DESC"
                                     }
                                  ]
                              },
                              "page": 0,
                              "size": 10
                            }""")
            })) @RequestBody SearchRequest request) {
        SearchSpecification<Email> specification = new SearchSpecification<>(request);
        return emailRepository.findAll(specification, request.getPageable());
    }

}
