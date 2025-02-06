package com.halilsahin.stockautomation.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        if (session.getAttribute("SPRING_SECURITY_CONTEXT") != null) { // Oturumda kullanıcı varsa
            return "redirect:/sales";
        } else { // Oturum yoksa
            return "redirect:/login";
        }
    }

}
