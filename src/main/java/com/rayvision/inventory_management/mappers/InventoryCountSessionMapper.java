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
    InventoryCountSessionDTO toDto(InventoryCountSession entity);

    // DTO -> SESSION
    @Mapping(source = "locationId", target = "location.id")
    @Mapping(target = "lines", ignore = true)
    InventoryCountSession toEntity(InventoryCountSessionDTO dto);

    // LINES -> DTO
    @Mapping(source = "inventoryItem.id", target = "inventoryItemId")
    // NEW: subRecipe
    @Mapping(source = "subRecipe.id", target = "subRecipeId")
    @Mapping(source = "storageArea.id", target = "storageAreaId")
    @Mapping(source = "countUom.id", target = "countUomId")
    InventoryCountLineDTO toLineDto(InventoryCountLine line);

    // DTO -> LINES
    @Mapping(source = "inventoryItemId", target = "inventoryItem.id")
    // NEW: subRecipe
    @Mapping(source = "subRecipeId", target = "subRecipe.id")
    @Mapping(source = "storageAreaId", target = "storageArea.id")
    @Mapping(source = "countUomId", target = "countUom.id")
    InventoryCountLine toLineEntity(InventoryCountLineDTO dto);

}
