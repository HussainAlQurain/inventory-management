package com.rayvision.inventory_management.mappers.impl;

import com.rayvision.inventory_management.model.PurchaseOption;
import com.rayvision.inventory_management.model.dto.PurchaseOptionDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class PurchaseOptionMapper extends AbstractMapper<PurchaseOption, PurchaseOptionDTO>{

    public PurchaseOptionMapper(ModelMapper modelMapper) {
        super(modelMapper);
    }

    @Override
    protected Class<PurchaseOption> getEntityClass() {
        return PurchaseOption.class;
    }

    @Override
    protected Class<PurchaseOptionDTO> getDtoClass() {
        return PurchaseOptionDTO.class;
    }

}
