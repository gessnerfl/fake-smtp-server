package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.repository.EmailAttachmentRepository;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import de.gessnerfl.fakesmtp.util.MediaTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.ServletContext;
import jakarta.validation.constraints.Min;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class EmailRestController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final String DEFAULT_SORT_PROPERTY = "receivedOn";

    private final EmailRepository emailRepository;
    private final EmailAttachmentRepository emailAttachmentRepository;
    private final MediaTypeUtil mediaTypeUtil;
    private final ServletContext servletContext;

    @Autowired
    public EmailRestController(EmailRepository emailRepository, EmailAttachmentRepository emailAttachmentRepository, MediaTypeUtil mediaTypeUtil, ServletContext servletContext) {
        this.emailRepository = emailRepository;
        this.emailAttachmentRepository = emailAttachmentRepository;
        this.mediaTypeUtil = mediaTypeUtil;
        this.servletContext = servletContext;
    }

    @GetMapping("/email")
    public List<Email> all(@RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
                           @RequestParam(value = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(0) int size,
                           @RequestParam(value = "sort", defaultValue = "DESC") Sort.Direction sort) {
        var result = emailRepository.findAll(PageRequest.of(page, size, Sort.by(sort, DEFAULT_SORT_PROPERTY)));
        if (result.getNumber() != 0 && result.getNumber() >= result.getTotalPages()) {
            return Collections.emptyList();
        }
        return result.getContent();
    }

    @GetMapping("/email/{id}")
    public Email getEmailById(@PathVariable Long id) {
        return emailRepository.findById(id).orElseThrow(() -> new EmailNotFoundException("Could not find email " + id));
    }

    @GetMapping("/email/{mailId}/attachment/{attachmentId}")
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

    @DeleteMapping("/email/{id}")
    public void deleteEmailById(@PathVariable Long id) {
        emailRepository.deleteById(id);
        emailRepository.flush();
    }

    @DeleteMapping("/email")
    public void deleteAllEmails() {
        emailAttachmentRepository.deleteAllInBatch();
        emailRepository.deleteAllInBatch();
        emailRepository.flush();
    }

}