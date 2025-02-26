package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.StorageArea;
import com.rayvision.inventory_management.model.dto.StorageAreaDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StorageAreaMapper {
    // From Entity to DTO
    @Mapping(source = "location.id", target = "locationId")
    StorageAreaDTO toDto(StorageArea entity);

    // From DTO to Entity
    @Mapping(source = "locationId", target = "location.id")
    StorageArea toEntity(StorageAreaDTO dto);
}
