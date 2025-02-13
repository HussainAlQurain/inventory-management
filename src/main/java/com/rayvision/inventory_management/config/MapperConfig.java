package com.rayvision.inventory_management.config;

import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.PurchaseOption;
import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.model.dto.InventoryItemCreateDTO;
import com.rayvision.inventory_management.model.dto.PurchaseOptionDTO;
import com.rayvision.inventory_management.model.dto.UserDTO;
import org.modelmapper.*;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.modelmapper.config.Configuration.AccessLevel.PRIVATE;

@Configuration
public class MapperConfig {
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.LOOSE)
                .setSkipNullEnabled(true)
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(PRIVATE);

        modelMapper.typeMap(InventoryItemCreateDTO.class, InventoryItem.class)
                .addMappings(mapper -> {
                    mapper.skip(InventoryItem::setPurchaseOptions);
                });

        return modelMapper;
    }
}
