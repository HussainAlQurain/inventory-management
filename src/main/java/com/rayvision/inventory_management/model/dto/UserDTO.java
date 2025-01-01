package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String username;
    private String password;
    private String email;
    private String status;
    private String firstName;
    private String lastName;
    private String phone;
}
