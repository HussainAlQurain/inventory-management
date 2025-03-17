package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.UnitOfMeasureCategory;
import com.rayvision.inventory_management.model.dto.UnitOfMeasureCategoryCreateDTO;
import com.rayvision.inventory_management.model.dto.UnitOfMeasureCategoryResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UnitOfMeasureCategoryMapper {
    UnitOfMeasureCategoryResponseDTO toResponseDTO(UnitOfMeasureCategory entity);

    // When creating, you can map from DTO to entity.
    // Note: Company is not set here; you’ll attach it in your service.
    UnitOfMeasureCategory fromCreateDTO(UnitOfMeasureCategoryCreateDTO dto);

}
