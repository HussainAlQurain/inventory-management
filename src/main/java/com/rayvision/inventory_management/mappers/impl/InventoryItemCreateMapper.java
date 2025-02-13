package com.rayvision.inventory_management.mappers.impl;

import com.rayvision.inventory_management.mappers.Mapper;
import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.dto.InventoryItemCreateDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class InventoryItemCreateMapper extends AbstractMapper<InventoryItem, InventoryItemCreateDTO> {

    public InventoryItemCreateMapper(ModelMapper modelMapper) {
        super(modelMapper);
        // Skip purchaseOptions in this mapper so it doesn't attempt to convert them
        modelMapper.typeMap(InventoryItemCreateDTO.class, InventoryItem.class)
                .addMappings(mapper -> mapper.skip(InventoryItem::setPurchaseOptions));
    }

    @Override
    protected Class<InventoryItem> getEntityClass() {
        return InventoryItem.class;
    }

    @Override
    protected Class<InventoryItemCreateDTO> getDtoClass() {
        return InventoryItemCreateDTO.class;
    }

}
