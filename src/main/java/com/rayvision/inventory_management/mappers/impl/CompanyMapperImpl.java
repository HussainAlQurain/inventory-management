package com.rayvision.inventory_management.mappers.impl;

import com.rayvision.inventory_management.mappers.Mapper;
import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.model.dto.CompanyDTO;
import com.rayvision.inventory_management.model.dto.UserDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CompanyMapperImpl implements Mapper<Company, CompanyDTO> {
    private final ModelMapper modelMapper;
    private final Mapper<Users, UserDTO> userMapper;

    public CompanyMapperImpl(ModelMapper modelMapper, Mapper<Users, UserDTO> userMapper) {
        this.modelMapper = modelMapper;
        this.userMapper = userMapper;
    }

    @Override
    public CompanyDTO mapTo(Company company) {
        CompanyDTO companyDTO = modelMapper.map(company, CompanyDTO.class);
        companyDTO.setUsers(company.getUsers().stream().map(userMapper::mapTo).collect(Collectors.toList()));
        return companyDTO;
    }

    @Override
    public Company mapFrom(CompanyDTO companyDTO) {
        Company company = modelMapper.map(companyDTO, Company.class);
        company.setUsers(companyDTO.getUsers().stream().map(userMapper::mapFrom).collect(Collectors.toSet()));
        return company;
    }
}
