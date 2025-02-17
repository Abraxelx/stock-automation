package com.halilsahin.stockautomation.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Kullanıcı adı veya şifre hatalı!");
        }
        return "login";
    }

    @GetMapping("/logout")
    public String redirectToLogin() {
        return "redirect:/login";
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
