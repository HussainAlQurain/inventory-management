package com.rayvision.inventory_management.model.dto;

import com.rayvision.inventory_management.enums.userRoles;
import lombok.Data;

@Data
public class UserUpdateDTO {
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String status; // can be used to enable/disable
    private userRoles role; // optional role update
}