package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.repository.EmailAttachmentRepository;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import de.gessnerfl.fakesmtp.util.MediaTypeUtil;

import jakarta.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/emails")
@Validated
public class EmailRestController {

    private static final String DEFAULT_SORT_PROPERTY = "receivedOn";

    private final EmailRepository emailRepository;
    private final EmailAttachmentRepository emailAttachmentRepository;
    private final MediaTypeUtil mediaTypeUtil;
    private final ServletContext servletContext;

    @Autowired
    public EmailRestController(EmailRepository emailRepository,
                               EmailAttachmentRepository emailAttachmentRepository,
                               MediaTypeUtil mediaTypeUtil,
                               ServletContext servletContext) {
        this.emailRepository = emailRepository;
        this.emailAttachmentRepository = emailAttachmentRepository;
        this.mediaTypeUtil = mediaTypeUtil;
        this.servletContext = servletContext;
    }

    @GetMapping()
    public Page<Email> all(@SortDefault(sort = DEFAULT_SORT_PROPERTY, direction = Sort.Direction.DESC) Pageable pageable)
    {
        return emailRepository.findAll(pageable);
    }

    @GetMapping("/{id}")
    public Email getEmailById(@PathVariable Long id) {
        return emailRepository.findById(id).orElseThrow(() -> new EmailNotFoundException("Could not find email " + id));
    }

    @GetMapping("/{mailId}/attachment/{attachmentId}")
    @ResponseBody
    public ResponseEntity<ByteArrayResource> getEmailAttachmentById(@PathVariable Long mailId, @PathVariable Long attachmentId) {
        var attachment = emailAttachmentRepository.findById(attachmentId)
                .filter(a -> a.getEmail().getId().equals(mailId))
                .orElseThrow(() -> new AttachmentNotFoundException("Attachment with id " + attachmentId + " not found for mail " + mailId));

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

}