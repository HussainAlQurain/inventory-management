package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class UserDto {
    private String username;
    private String password;
    private String email;
    private boolean active = true;
}
