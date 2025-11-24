package com.bim.seif.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class SpaForwardController {

    @GetMapping("/{path:[^\\.]*}")
    public String forwardTopLevel() {
        return "forward:/ext/index.html";
    }

    @GetMapping("/{path:^(?!api|docs|swagger-ui|v3|auth|archivos|error$).*$}/{rest:[^\\.]*}")
    public String forwardNested() {
        return "forward:/ext/index.html";
    }

    @GetMapping({ "/", "/ext", "/ext/" })
    public String forwardRoot() {
        return "forward:/ext/index.html";
    }
}