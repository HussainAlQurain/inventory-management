package com.rayvision.inventory_management.mappers.impl;

import com.rayvision.inventory_management.mappers.Mapper;
import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.model.CompanyUser;
import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.model.dto.CompanyDTO;
import com.rayvision.inventory_management.model.dto.UserDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CompanyMapperImpl implements Mapper<Company, CompanyDTO> {
    private final ModelMapper modelMapper;

    public CompanyMapperImpl(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;

        // Configure specific mappings
        modelMapper.typeMap(Company.class, CompanyDTO.class)
                .addMappings(mapper -> {
                    mapper.map(Company::getTaxId, CompanyDTO::setTaxId);
                    mapper.skip(CompanyDTO::setUsers); // Skip users - we'll handle them manually
                });
    }

    @Override
    public CompanyDTO mapTo(Company company) {
        if (company == null) return null;

        CompanyDTO dto = modelMapper.map(company, CompanyDTO.class);

        // Manually map users
        if (company.getCompanyUsers() != null) {
            List<UserDTO> userDTOs = company.getCompanyUsers().stream()
                    .map(cu -> {
                        UserDTO userDTO = new UserDTO();
                        Users user = cu.getUser();
                        userDTO.setUsername(user.getUsername());
                        userDTO.setEmail(user.getEmail());
                        userDTO.setFirstName(user.getFirstName());
                        userDTO.setLastName(user.getLastName());
                        userDTO.setPhone(user.getPhone());
                        userDTO.setStatus(user.getStatus());
                        return userDTO;
                    })
                    .collect(Collectors.toList());
            dto.setUsers(userDTOs);
        }

        return dto;
    }

    @Override
    public Company mapFrom(CompanyDTO dto) {
        if (dto == null) return null;
        return modelMapper.map(dto, Company.class);
    }
}
