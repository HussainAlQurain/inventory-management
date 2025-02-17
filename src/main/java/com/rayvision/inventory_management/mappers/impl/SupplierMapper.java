package com.rayvision.inventory_management.mappers.impl;

import com.rayvision.inventory_management.model.Supplier;
import com.rayvision.inventory_management.model.dto.SupplierDTO;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper extends AbstractMapper<Supplier, SupplierDTO> {

    public SupplierMapper(ModelMapper modelMapper) {
        super(modelMapper);
        // Tell ModelMapper how to map SupplierDTO -> Supplier explicitly
        modelMapper.addMappings(new PropertyMap<SupplierDTO, Supplier>() {
            @Override
            protected void configure() {
                // Fields we don't want mapped automatically
                skip(destination.getId());
                skip(destination.getDefaultCategory());
                skip(destination.getCompany());

                // Now map each field we do want:
                map().setName(source.getName());
                map().setCustomerNumber(source.getCustomerNumber());
                map().setMinimumOrder(source.getMinimumOrder());
                map().setTaxId(source.getTaxId());
                map().setTaxRate(source.getTaxRate());
                map().setPaymentTerms(source.getPaymentTerms());
                map().setComments(source.getComments());
                map().setAddress(source.getAddress());
                map().setCity(source.getCity());
                map().setState(source.getState());
                map().setZip(source.getZip());
                map().setCcEmails(source.getCcEmails());

                // If your DTO has more fields (like "phones", "emails", etc.) that
                // you'd like to map directly, you can handle them here, or skip them
                // and handle them in the facade. For example:
                // skip(destination.getOrderEmails());
                // skip(destination.getOrderPhones());
            }
        });


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
