package com.example.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Collection;

@Controller
public class Client1Controller {

    @Resource
    private TokenStore tokenStore;

    @Value("${security.oauth2.client.clientId}")
    private String clientId;

    public ModelAndView home() {
        return new ModelAndView("index");
    }

    // 需要开启方法级别保护
    //@PreAuthorize("hasRole('USER')")
    @RequestMapping("/user")
    public ModelAndView user() {
        return new ModelAndView("user");
    }

    // 需要开启方法级别保护
    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping("/admin")
    public ModelAndView admin() {
        return new ModelAndView("admin");
    }


    /**
     * /api 受资源服务器保护，验证token，请求时需要带上
     */
    @GetMapping("/api/getUserInfo")
    @ResponseBody
    public Principal getUserInfo() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * /api2 受Spring Security保护，验证cookie和session，需要先通过登录
     */
    @GetMapping("/api2/getUserInfo")
    @ResponseBody
    public Principal getUserInfo2() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
