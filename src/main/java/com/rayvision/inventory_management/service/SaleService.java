package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.SaleCreateDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleService {
    Sale createSale(SaleCreateDTO dto);
    List<Sale> findSalesByLocationAndDateRange(Long locationId, LocalDateTime start, LocalDateTime end);
    Optional<Sale> getSaleById(Long saleId);


}
