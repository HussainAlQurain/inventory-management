package com.rayvision.inventory_management.model.dto;

import com.rayvision.inventory_management.model.Company;
import lombok.Data;

@Data
public class UserDto {
    private String username;
    private String password;
    private String email;
    private String status;
    private String firstName;
    private String lastName;
    private String phone;
}
