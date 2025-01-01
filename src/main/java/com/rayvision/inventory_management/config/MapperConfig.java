package com.rayvision.inventory_management.config;

import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.model.dto.UserDTO;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);

        // Custom mapping configuration for CreateUserDTO to Users
        modelMapper.addMappings(new PropertyMap<UserDTO, Users>() {
            @Override
            protected void configure() {
                // Map fields from DTO to entity
                map().setUsername(source.getUsername());
                map().setPassword(source.getPassword());
                map().setEmail(source.getEmail());
                map().setStatus(source.getStatus());
                map().setFirstName(source.getFirstName());
                map().setLastName(source.getLastName());
                map().setPhone(source.getPhone());
                skip(destination.getCompanies());
                skip(destination.getRoles());
            }
        });

        return modelMapper;
    }
}
