package de.gessnerfl.fakesmtp.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Controller
@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
public class UiRouteController {

    @GetMapping({"/"})
    public String index() {
        return "index";
    }

    @GetMapping("/emails/**")
    public String emails(HttpServletRequest request) {
        return "index";
    }

}
