package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<String> login(@RequestBody Users user) {
        String authenticatedUser = userService.verify(user);
        return ResponseEntity.ok(authenticatedUser);
    }
}
