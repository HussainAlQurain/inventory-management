package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.UnitOfMeasure;
import com.rayvision.inventory_management.model.dto.UomFilterOptionDTO;
import com.rayvision.inventory_management.repository.CompanyRepository;
import com.rayvision.inventory_management.repository.UnitOfMeasureRepository;
import com.rayvision.inventory_management.service.UnitOfMeasureService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UnitOfMeasureServiceImpl implements UnitOfMeasureService {
    private final UnitOfMeasureRepository uomRepository;
    private final CompanyRepository companyRepository;

    public UnitOfMeasureServiceImpl(UnitOfMeasureRepository uomRepository, CompanyRepository companyRepository) {
        this.uomRepository = uomRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public List<UnitOfMeasure> getAllUnitOfMeasures(Long companyId) {
        return uomRepository.findByCompanyId(companyId);
    }

    @Override
    public Optional<UnitOfMeasure> getById(Long companyId, Long id) {
        return uomRepository.findByCompanyIdAndId(companyId, id);
    }

    @Override
    public UnitOfMeasure save(Long companyId, UnitOfMeasure uom) {
        return companyRepository.findById(companyId).map(company -> {
            uom.setCompany(company);
            return uomRepository.save(uom);
        }).orElseThrow(() -> new RuntimeException("Invalid Company ID: " + companyId));
    }

    @Override
    public UnitOfMeasure update(Long companyId, UnitOfMeasure uom) {
        return uomRepository.findByCompanyIdAndId(companyId, uom.getId())
                .map(existing -> {
                    existing.setName(uom.getName());
                    existing.setAbbreviation(uom.getAbbreviation());
                    existing.setCategory(uom.getCategory());
                    existing.setConversionFactor(uom.getConversionFactor());
                    return uomRepository.save(existing);
                }).orElseThrow(() -> new RuntimeException("UnitOfMeasure not found for company: " + companyId));
    }

    @Override
    public UnitOfMeasure partialUpdate(Long companyId, UnitOfMeasure uom) {
        return uomRepository.findByCompanyIdAndId(companyId, uom.getId()).map(existingUom -> {
            Optional.ofNullable(uom.getName()).ifPresent(existingUom::setName);
            Optional.ofNullable(uom.getAbbreviation()).ifPresent(existingUom::setAbbreviation);
            Optional.ofNullable(uom.getCategory()).ifPresent(existingUom::setCategory);
            Optional.ofNullable(uom.getConversionFactor()).ifPresent(existingUom::setConversionFactor);
            return uomRepository.save(existingUom);
        }).orElseThrow(() -> new RuntimeException("Could not find UnitOfMeasure with id: " + uom.getId()));
    }

    @Override
    public void deleteUnitOfMeasureById(Long companyId, Long id) {
        UnitOfMeasure uom = uomRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new RuntimeException("UnitOfMeasure not found for company: " + companyId));
        uomRepository.delete(uom);
    }

    @Override
    public Page<UomFilterOptionDTO> findPaginatedFilterOptions(Long companyId, String search, Pageable pageable) {
        if (search == null) search = "";
        return uomRepository.findPaginatedFilterOptions(companyId, search, pageable);
    }

}
