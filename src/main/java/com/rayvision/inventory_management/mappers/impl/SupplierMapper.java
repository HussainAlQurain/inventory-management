package com.rayvision.inventory_management.mappers.impl;

import com.rayvision.inventory_management.model.Supplier;
import com.rayvision.inventory_management.model.dto.SupplierDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper extends AbstractMapper<Supplier, SupplierDTO> {

    public SupplierMapper(ModelMapper modelMapper) {
        super(modelMapper);
    }

    @Override
    protected Class<Supplier> getEntityClass() {
        return Supplier.class;
    }

    @Override
    protected Class<SupplierDTO> getDtoClass() {
        return SupplierDTO.class;
    }

}
