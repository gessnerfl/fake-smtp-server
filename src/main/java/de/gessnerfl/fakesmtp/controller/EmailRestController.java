package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class EmailRestController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final String DEFAULT_SORT_PROPERTY = "receivedOn";

    private final EmailRepository emailRepository;

    @Autowired
    public EmailRestController(EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
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

    @DeleteMapping("/email/{id}")
    public void deleteEmailById(@PathVariable Long id) {
        emailRepository.deleteById(id);
        emailRepository.flush();
    }

}