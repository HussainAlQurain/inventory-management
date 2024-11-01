package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("create")
    public Users createUser(@RequestBody Users user) {
        return userService.createUser(user);
    }

    @PostMapping("login")
    public String login(@RequestBody Users user) {
        return "Success";
    }
}
