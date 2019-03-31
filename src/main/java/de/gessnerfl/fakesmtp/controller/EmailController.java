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
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletContext;

@Controller
@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
public class EmailController {
    private static final Sort DEFAULT_SORT = new Sort(Sort.Direction.DESC, "receivedOn");
    private static final int DEFAULT_PAGE_SIZE = 10;
    static final String EMAIL_LIST_VIEW = "email-list";
    static final String EMAIL_LIST_MODEL_NAME = "mails";
    static final String SINGLE_EMAIL_VIEW = "email";
    static final String SINGLE_EMAIL_MODEL_NAME = "mail";
    static final String REDIRECT_EMAIL_LIST_VIEW = "redirect:/email";

    private final EmailRepository emailRepository;
    private final EmailAttachmentRepository emailAttachmentRepository;
    private final MediaTypeUtil mediaTypeUtil;
    private final ServletContext servletContext;

    @Autowired
    public EmailController(EmailRepository emailRepository, EmailAttachmentRepository emailAttachmentRepository, MediaTypeUtil mediaTypeUtil, ServletContext servletContext) {
        this.emailRepository = emailRepository;
        this.emailAttachmentRepository = emailAttachmentRepository;
        this.mediaTypeUtil = mediaTypeUtil;
        this.servletContext = servletContext;
    }

    @GetMapping({"/", "/email"})
    public String getAll(@RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(value = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) int size, Model model) {
        return getAllEmailsPaged(page, size, model);
    }

    private String getAllEmailsPaged(int page, int size, Model model) {
        if(page < 0 || size <= 0){
            return REDIRECT_EMAIL_LIST_VIEW;
        }
        var result = emailRepository.findAll(PageRequest.of(page, size, DEFAULT_SORT));
        if (result.getNumber() != 0 && result.getNumber() >= result.getTotalPages()) {
            return REDIRECT_EMAIL_LIST_VIEW;
        }
        model.addAttribute(EMAIL_LIST_MODEL_NAME, result);
        return EMAIL_LIST_VIEW;
    }

    @GetMapping("/email/{id}")
    public String getEmailById(@PathVariable Long id, Model model) {
        return emailRepository.findById(id).map(email -> appendToModelAndReturnView(model, email)).orElse(REDIRECT_EMAIL_LIST_VIEW);
    }

    private String appendToModelAndReturnView(Model model, Email email) {
        model.addAttribute(SINGLE_EMAIL_MODEL_NAME, email);
        return SINGLE_EMAIL_VIEW;
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
    public String deleteEmailById(@PathVariable Long id) {
        emailRepository.deleteById(id);
        emailRepository.flush();
        return REDIRECT_EMAIL_LIST_VIEW;
    }

}
