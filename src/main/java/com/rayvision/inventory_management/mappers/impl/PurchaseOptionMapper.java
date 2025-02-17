package com.rayvision.inventory_management.mappers.impl;

import com.rayvision.inventory_management.model.PurchaseOption;
import com.rayvision.inventory_management.model.dto.PurchaseOptionDTO;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

@Component
public class PurchaseOptionMapper extends AbstractMapper<PurchaseOption, PurchaseOptionDTO>{

    public PurchaseOptionMapper(ModelMapper modelMapper) {
        super(modelMapper);
        // Create an explicit PropertyMap to avoid ambiguity.
        modelMapper.addMappings(new PropertyMap<PurchaseOptionDTO, PurchaseOption>() {
            @Override
            protected void configure() {
                // Skip by calling the destination *getter*
                skip(destination.getId());
                skip(destination.getInventoryItem());
                skip(destination.getOrderingUom());
                skip(destination.getSupplier());
                // Now explicitly map the fields you do want
                map().setPrice(source.getPrice());
                map().setTaxRate(source.getTaxRate());
                map().setInnerPackQuantity(source.getInnerPackQuantity());
                map().setPacksPerCase(source.getPacksPerCase());
                map().setMinOrderQuantity(source.getMinOrderQuantity());
                map().setMainPurchaseOption(source.isMainPurchaseOption());
                map().setOrderingEnabled(source.isOrderingEnabled());
                map().setSupplierProductCode(source.getSupplierProductCode());
                map().setNickname(source.getNickname());
                map().setScanBarcode(source.getScanBarcode());
            }
        });
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
