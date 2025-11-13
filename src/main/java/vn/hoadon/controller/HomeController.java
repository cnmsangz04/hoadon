package vn.hoadon.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home() {
        // forward tới index.html Vue SPA
        return "forward:/index.html";
    }
}
