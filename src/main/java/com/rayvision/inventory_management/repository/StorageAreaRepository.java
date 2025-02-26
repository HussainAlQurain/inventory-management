package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.StorageArea;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StorageAreaRepository extends CrudRepository<StorageArea, Long> {
}
