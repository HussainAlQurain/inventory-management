package com.rayvision.inventory_management.mappers.impl;

import com.rayvision.inventory_management.model.UnitOfMeasureCategory;
import com.rayvision.inventory_management.model.dto.UnitOfMeasureCategoryCreateDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class UnitOfMeasureCategoryMapper extends AbstractMapper<UnitOfMeasureCategory, UnitOfMeasureCategoryCreateDTO> {
    public UnitOfMeasureCategoryMapper(ModelMapper modelMapper) {
        super(modelMapper);
    }

    @Override
    protected Class<UnitOfMeasureCategory> getEntityClass() {
        return UnitOfMeasureCategory.class;
    }

    @Override
    protected Class<UnitOfMeasureCategoryCreateDTO> getDtoClass() {
        return UnitOfMeasureCategoryCreateDTO.class;
    }

}
