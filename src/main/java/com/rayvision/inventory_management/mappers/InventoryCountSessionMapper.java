package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.InventoryCountLine;
import com.rayvision.inventory_management.model.InventoryCountSession;
import com.rayvision.inventory_management.model.dto.InventoryCountLineDTO;
import com.rayvision.inventory_management.model.dto.InventoryCountSessionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryCountSessionMapper {

    // SESSION -> DTO
    @Mapping(source = "location.id", target = "locationId")
    // The lines property will be mapped automatically if we do a mapping method for lines
    InventoryCountSessionDTO toDto(InventoryCountSession entity);

    // DTO -> SESSION
    @Mapping(source = "locationId", target = "location.id")
    @Mapping(target = "lines", ignore = true) // We'll handle lines in the service
    InventoryCountSession toEntity(InventoryCountSessionDTO dto);

    // LINES -> DTO
    @Mapping(source = "inventoryItem.id", target = "inventoryItemId")
    @Mapping(source = "storageArea.id", target = "storageAreaId")
    @Mapping(source = "countUom.id", target = "countUomId")
    InventoryCountLineDTO toLineDto(InventoryCountLine line);

    // DTO -> LINES
    @Mapping(source = "inventoryItemId", target = "inventoryItem.id")
    @Mapping(source = "storageAreaId", target = "storageArea.id")
    @Mapping(source = "countUomId", target = "countUom.id")
    InventoryCountLine toLineEntity(InventoryCountLineDTO dto);

}
