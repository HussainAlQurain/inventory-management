package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.Assortment;
import com.rayvision.inventory_management.model.dto.AssortmentDTO;

import java.util.List;

public interface AssortmentService {
    Assortment create(Long companyId, AssortmentDTO dto);
    Assortment update(Long companyId, Long assortmentId, AssortmentDTO dto);
    Assortment partialUpdate(Long companyId, Long assortmentId, AssortmentDTO dto);
    void delete(Long companyId, Long assortmentId);

    Assortment getOne(Long companyId, Long assortmentId);
    List<Assortment> getAll(Long companyId);
}
