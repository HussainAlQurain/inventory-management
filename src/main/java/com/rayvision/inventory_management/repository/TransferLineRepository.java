package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.TransferLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferLineRepository extends JpaRepository<TransferLine, Long> {

}
