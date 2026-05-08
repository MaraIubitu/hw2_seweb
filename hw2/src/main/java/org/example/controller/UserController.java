package org.example.controller;

import org.example.service.RdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private RdfService rdfService;

    @GetMapping
    public String users(Model model, HttpSession session) {
        model.addAttribute("users", rdfService.getAllUsers());
        String currentUserUri = (String) session.getAttribute("currentUserUri");
        if (currentUserUri != null) {
            model.addAttribute("currentUser", rdfService.getUserByUri(currentUserUri));
        }
        return "users";
    }

    @PostMapping("/login/{userId}")
    public String login(@PathVariable String userId, HttpSession session) {
        session.setAttribute("currentUserUri", "http://example.org/book/" + userId);
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("currentUserUri");
        return "redirect:/users";
    }
}