package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.dto.LocationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    @Mapping(source = "company.id", target = "companyId")
    LocationDTO toDTO(Location location);

    @Mapping(source = "companyId", target = "company.id")
    Location toEntity(LocationDTO dto);
}
