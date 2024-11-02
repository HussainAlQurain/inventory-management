package com.rayvision.inventory_management.mappers.impl;

import com.rayvision.inventory_management.mappers.Mapper;
import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.model.dto.UserDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class UserMapperImpl implements Mapper<Users, UserDto> {

    private final ModelMapper modelMapper;

    public UserMapperImpl(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public UserDto mapTo(Users userEntity) {
        return modelMapper.map(userEntity, UserDto.class);
    }

    @Override
    public Users mapFrom(UserDto userDto) {
        return modelMapper.map(userDto, Users.class);
    }

}
