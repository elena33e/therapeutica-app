package com.therapeutica.therapeutica_app.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home/index";
    }

    @GetMapping("/login")
    public String login() {
        // Aceasta este metoda apelată de Spring Security când cineva e neautentificat
        return "auth/login";
    }

}