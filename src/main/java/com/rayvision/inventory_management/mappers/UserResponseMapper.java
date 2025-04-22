package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.Role;
import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.model.dto.UserResponseDTO;
import com.rayvision.inventory_management.repository.CompanyUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserResponseMapper {
    
    @Autowired
    private CompanyUserRepository companyUserRepository;
    
    public UserResponseDTO toDto(Users user) {
        if (user == null) {
            return null;
        }
        
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setStatus(user.getStatus());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        
        // Convert roles to strings
        if (user.getRoles() != null) {
            Set<String> roleNames = user.getRoles().stream()
                    .map(Role::getName)
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            dto.setRoles(roleNames);
        }
        
        // Get company IDs for this user
        List<Long> companyIds = companyUserRepository.findCompanyIdsByUserId(user.getId());
        dto.setCompanyIds(Set.copyOf(companyIds));
        
        return dto;
    }
    
    public List<UserResponseDTO> toDtoList(List<Users> users) {
        if (users == null) {
            return null;
        }
        
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}