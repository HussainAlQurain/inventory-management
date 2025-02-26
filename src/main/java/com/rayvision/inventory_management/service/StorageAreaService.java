package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.StorageArea;

import java.util.Optional;

public interface StorageAreaService {
    void save(StorageArea storageArea);
    void update(StorageArea storageArea);
    void delete(StorageArea storageArea);
    Optional<StorageArea> getStorageArea(int id);
}
