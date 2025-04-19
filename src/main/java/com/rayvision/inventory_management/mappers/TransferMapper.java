package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.Transfer;
import com.rayvision.inventory_management.model.TransferLine;
import com.rayvision.inventory_management.model.dto.TransferDTO;
import com.rayvision.inventory_management.model.dto.TransferLineDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransferMapper {
    /* -------- Transfer‐header -------- */
    @Mapping(source = "fromLocation.id",   target = "fromLocationId")
    @Mapping(source = "fromLocation.name", target = "fromLocationName")
    @Mapping(source = "toLocation.id",     target = "toLocationId")
    @Mapping(source = "toLocation.name",   target = "toLocationName")
    @Mapping(target = "lines", expression = "java(toLineDtoList(t.getLines()))")
    TransferDTO toDto(Transfer t);

    /* ------------- lines ------------- */
    @Mapping(source = "inventoryItem.id",           target = "inventoryItemId")
    @Mapping(source = "subRecipe.id",      target = "subRecipeId")
    /* decide which name to use */
    @Mapping(target = "itemName",  expression = "java(lineName(l))")
    @Mapping(source = "unitOfMeasure.id",  target = "unitOfMeasureId")
    @Mapping(source = "unitOfMeasure.name",target = "uomName")
    TransferLineDTO toLineDto(TransferLine l);

    /* ---------- helpers -------------- */
    default List<TransferLineDTO> toLineDtoList(List<TransferLine> lines){
        return (lines == null) ? List.of()
                : lines.stream().map(this::toLineDto).toList();
    }

    /** choose sub‑recipe name first; else inventory item name */
    default String lineName(TransferLine l){
        if (l == null) return null;
        if (l.getSubRecipe() != null) return l.getSubRecipe().getName();
        if (l.getInventoryItem()      != null) return l.getInventoryItem().getName();
        return null;
    }

}
