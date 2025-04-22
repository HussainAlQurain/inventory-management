package com.rayvision.inventory_management.model.dto;

import com.rayvision.inventory_management.enums.userRoles;
import lombok.Data;

import java.util.Set;

@Data
public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    private String status;
    private String firstName;
    private String lastName;
    private String phone;
    private Set<String> roles;
    private Set<Long> companyIds;
}