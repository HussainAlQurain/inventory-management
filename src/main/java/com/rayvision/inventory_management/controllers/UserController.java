package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.impl.UserLoginMapperImpl;
import com.rayvision.inventory_management.mappers.impl.UserMapperImpl;
import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.model.dto.LoginDTO;
import com.rayvision.inventory_management.model.dto.UserDTO;
import com.rayvision.inventory_management.service.impl.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    private UserMapperImpl userMapper;

    @Autowired
    private UserLoginMapperImpl userLoginMapper;


    @PostMapping("create")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDto) {
        // Map UserDto to Users entity
        Users user = userMapper.mapFrom(userDto);

        // Create user in service
        Users savedUser = userService.createUser(user);

        // Map created user to UserDto for response
        UserDTO responseUserDto = userMapper.mapTo(savedUser);
        responseUserDto.setPassword(null); // Set password to null to prevent returning it in response

        return ResponseEntity.ok(responseUserDto);
    }

    @PostMapping("login")
    public ResponseEntity<String> login(@RequestBody LoginDTO loginDTO) {
        try {
            Users user = userLoginMapper.mapFrom(loginDTO);
            String authenticatedUser = userService.verify(user);
            return ResponseEntity.ok(authenticatedUser);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\": \"Invalid Username or Password\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\": \"Authentication failed\"}");
        }
    }


}
