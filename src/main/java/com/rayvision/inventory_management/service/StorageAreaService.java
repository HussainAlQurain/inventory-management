package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.StorageArea;
import com.rayvision.inventory_management.model.dto.StorageAreaDTO;

import java.util.List;
import java.util.Optional;

public interface StorageAreaService {
    StorageArea create(Long locationId, StorageAreaDTO dto);
    StorageArea update(Long locationId, Long storageAreaId, StorageAreaDTO dto);
    void delete(Long locationId, Long storageAreaId);
    StorageArea getOne(Long locationId, Long storageAreaId);
    List<StorageArea> getAllByLocation(Long locationId);
}
