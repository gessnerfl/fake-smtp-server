package de.gessnerfl.fakesmtp.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.query.SearchRequest;
import de.gessnerfl.fakesmtp.model.query.SearchSpecification;
import de.gessnerfl.fakesmtp.repository.EmailAttachmentRepository;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
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

    private static final String DEFAULT_SORT_PROPERTY = "receivedOn";

    private final EmailRepository emailRepository;
    private final EmailAttachmentRepository emailAttachmentRepository;
    private final MediaTypeUtil mediaTypeUtil;
    private final ServletContext servletContext;
    private final EmailSseEmitterService emailSseEmitterService;

    @Autowired
    public EmailRestController(EmailRepository emailRepository,
            EmailAttachmentRepository emailAttachmentRepository,
            MediaTypeUtil mediaTypeUtil,
            ServletContext servletContext,
            EmailSseEmitterService emailSseEmitterService) {
        this.emailRepository = emailRepository;
        this.emailAttachmentRepository = emailAttachmentRepository;
        this.mediaTypeUtil = mediaTypeUtil;
        this.servletContext = servletContext;
        this.emailSseEmitterService = emailSseEmitterService;
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

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + attachment.getFilename())
                .contentType(mediaType)
                .contentLength(attachment.getData().length) //
                .body(new ByteArrayResource(attachment.getData()));
    }

    @DeleteMapping("/{id}")
    public void deleteEmailById(@PathVariable Long id) {
        emailRepository.deleteById(id);
        emailRepository.flush();
    }

    @DeleteMapping()
    public void deleteAllEmails() {
        emailAttachmentRepository.deleteAllInBatch();
        emailRepository.deleteAllInBatch();
        emailRepository.flush();
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToEmailEvents() {
        final var emitter = emailSseEmitterService.createAndAddEmitter();

        try {
            emitter.send(SseEmitter.event()
                    .name("connection-established")
                    .data("Connected to email events"));
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
