package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @Autowired
    public EmailController(EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }

    @RequestMapping({"/", "/email"})
    public String getAll(@RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(value = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) int size, Model model) {
        return getAllEmailsPaged(page, size, model);
    }

    private String getAllEmailsPaged(int page, int size, Model model) {
        if(page < 0 || size <= 0){
            return REDIRECT_EMAIL_LIST_VIEW;
        }
        Page<Email> result = emailRepository.findAll(new PageRequest(page, size, DEFAULT_SORT));
        if (result.getNumber() != 0 && result.getNumber() >= result.getTotalPages()) {
            return REDIRECT_EMAIL_LIST_VIEW;
        }
        model.addAttribute(EMAIL_LIST_MODEL_NAME, result);
        return EMAIL_LIST_VIEW;
    }

    @RequestMapping({"/email/{id}"})
    public String getEmailById(@PathVariable Long id, Model model) {
        Email email = emailRepository.findOne(id);
        if (email != null) {
            model.addAttribute(SINGLE_EMAIL_MODEL_NAME, email);
            return SINGLE_EMAIL_VIEW;
        }
        return REDIRECT_EMAIL_LIST_VIEW;
    }

}
