package de.gessnerfl.fakesmtp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Controller
@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
public class IndexController {

    @GetMapping({"/"})
    public String home() {
        return "index";
    }

}
