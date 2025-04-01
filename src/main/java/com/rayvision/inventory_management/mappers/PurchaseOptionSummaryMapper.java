package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.PurchaseOption;
import com.rayvision.inventory_management.model.dto.PurchaseOptionSummaryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PurchaseOptionSummaryMapper {
    @Mapping(target = "purchaseOptionId",       source = "id")
    @Mapping(target = "purchaseOptionNickname", source = "nickname")
    @Mapping(target = "price",                  source = "price")
    @Mapping(
            target = "orderingUomName",
            expression = "java(po.getOrderingUom() != null ? po.getOrderingUom().getName() : null)"
    )
    @Mapping(
            target = "inventoryItemName",
            expression = "java(po.getInventoryItem() != null ? po.getInventoryItem().getName() : null)"
    )
    @Mapping(
            target = "categoryName",
            expression = """
            java(
                po.getInventoryItem() != null
                && po.getInventoryItem().getCategory() != null
                ? po.getInventoryItem().getCategory().getName()
                : null
            )
        """
    )
    @Mapping(
            target = "supplierName",
            expression = "java(po.getSupplier() != null ? po.getSupplier().getName() : null)"
    )
    PurchaseOptionSummaryDTO toDto(PurchaseOption po);

    List<PurchaseOptionSummaryDTO> toDtoList(List<PurchaseOption> purchaseOptions);

}
