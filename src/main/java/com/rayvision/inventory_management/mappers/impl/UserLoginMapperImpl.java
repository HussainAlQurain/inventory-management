package com.rayvision.inventory_management.mappers.impl;

import com.rayvision.inventory_management.mappers.Mapper;
import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.model.dto.LoginDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class UserLoginMapperImpl implements Mapper<Users, LoginDTO> {

    private final ModelMapper modelMapper;

    public UserLoginMapperImpl(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public LoginDTO mapTo(Users entity) {
        return modelMapper.map(entity, LoginDTO.class);
    }

    @Override
    public Users mapFrom(LoginDTO dto) {
        Users user = new Users();
        user.setUsername(dto.username());
        user.setPassword(dto.password());
        return user;
    }
}
