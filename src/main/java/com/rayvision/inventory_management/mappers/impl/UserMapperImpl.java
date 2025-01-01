package com.rayvision.inventory_management.mappers.impl;

import com.rayvision.inventory_management.mappers.Mapper;
import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.model.dto.UserDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class UserMapperImpl implements Mapper<Users, UserDTO> {

    private final ModelMapper modelMapper;

    public UserMapperImpl(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public UserDTO mapTo(Users userEntity) {
        return modelMapper.map(userEntity, UserDTO.class);
    }

    @Override
    public Users mapFrom(UserDTO userDto) {
        return modelMapper.map(userDto, Users.class);
    }

}
