package com.rayvision.inventory_management.mappers.impl;

import com.rayvision.inventory_management.model.UnitOfMeasure;
import com.rayvision.inventory_management.model.UnitOfMeasureCategory;
import com.rayvision.inventory_management.model.dto.UnitOfMeasureCategoryCreateDTO;
import com.rayvision.inventory_management.model.dto.UnitOfMeasureCreateDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class UnitOfMeasureMapper extends AbstractMapper<UnitOfMeasure, UnitOfMeasureCreateDTO>{
    public UnitOfMeasureMapper(ModelMapper modelMapper) {
        super(modelMapper);
    }

    @Override
    protected Class<UnitOfMeasure> getEntityClass() {
        return UnitOfMeasure.class;
    }

    @Override
    protected Class<UnitOfMeasureCreateDTO> getDtoClass() {
        return UnitOfMeasureCreateDTO.class;
    }
}
