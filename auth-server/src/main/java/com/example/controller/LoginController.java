package com.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class LoginController {

    @RequestMapping("/loginPage")
    public ModelAndView loginPage() {
        return new ModelAndView("loginPage");
    }

}
