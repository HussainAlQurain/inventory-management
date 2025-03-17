package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.UnitOfMeasure;
import com.rayvision.inventory_management.model.dto.UnitOfMeasureResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UnitOfMeasureMapper {
    // Map from entity to DTO. (Assumes that your UnitOfMeasure entity has a 'category' field.)
    @Mapping(source = "category", target = "category")
    UnitOfMeasureResponseDTO toResponseDTO(UnitOfMeasure uom);
}
