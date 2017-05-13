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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
public class EmailController {
    private static final Sort DEFAULT_SORT = new Sort(Sort.Direction.DESC, "receivedOn");
    static final int DEFAULT_PAGE_SIZE = 25;
    static final String EMAIL_LIST_VIEW = "email-list";
    static final String EMAIL_LIST_MODEL_NAME = "mails";
    static final String SINGLE_EMAIL_VIEW = "email";
    static final String SINGLE_EMAIL_MODEL_NAME = "mail";
    static final String ERROR_MODEL_NAME = "error";
    static final String ERROR_MESSAGE_MODEL_NAME = "errorMessage";
    public static final String EMAIL_NOT_FOUND_MESSAGE = "Email with ID %d does not exist.";

    private final EmailRepository emailRepository;

    @Autowired
    public EmailController(EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }

    @RequestMapping({"/", "/email"})
    public ModelAndView getAll(@RequestParam(value = "p", defaultValue = "0") int page, @RequestParam(value = "s", defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        return getAllEmailsPaged(page, size);
    }

    private ModelAndView getAllEmailsPaged(int page, int size) {
        final int pageNumber = page < 0 ? 0 : page;
        final int pageSize = size < 0 ? DEFAULT_PAGE_SIZE : size;
        Page<Email> result = emailRepository.findAll(new PageRequest(pageNumber, pageSize, DEFAULT_SORT));
        if (result.getNumber() >= result.getTotalPages()) {
            result = emailRepository.findAll(new PageRequest(0, pageSize, DEFAULT_SORT));
        }
        return new ModelAndView(EMAIL_LIST_VIEW, EMAIL_LIST_MODEL_NAME, result);
    }

    @RequestMapping({"/email/{id}"})
    public ModelAndView getEmailById(@PathVariable Long id) {
        Email email = emailRepository.findOne(id);
        if (email != null) {
            return new ModelAndView(SINGLE_EMAIL_VIEW, SINGLE_EMAIL_MODEL_NAME, email);
        }
        ModelAndView result = getAllEmailsPaged(0, DEFAULT_PAGE_SIZE);
        result.addObject(ERROR_MODEL_NAME, true);
        result.addObject(ERROR_MESSAGE_MODEL_NAME, String.format(EMAIL_NOT_FOUND_MESSAGE, id));
        return result;
    }

}
