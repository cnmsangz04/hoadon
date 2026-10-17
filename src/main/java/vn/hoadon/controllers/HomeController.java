package vn.hoadon.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * Vue Router uses history mode, so a browser refresh asks Spring Boot for
     * the current UI path. Forward every UI route to the SPA entry point and
     * let Vue resolve the page client-side.
     */
    @GetMapping({
            "/",
            "/home",
            "/lookup-invoice",
            "/auth/**",
            "/register/**",
            "/form-invoice/**",
            "/invoice/**",
            "/invoice-packages/**",
            "/error-message/**",
            "/imports/**",
            "/categories/**",
            "/reports/**",
            "/email/**",
            "/setting/**",
            "/administrator/**"
    })
    public String home() {
        return "forward:/index.html";
    }

    @GetMapping("/favicon.ico")
    public String favicon() {
        return "redirect:/favicon.svg";
    }
}
