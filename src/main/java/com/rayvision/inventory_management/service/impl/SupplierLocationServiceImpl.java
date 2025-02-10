package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.Supplier;
import com.rayvision.inventory_management.model.SupplierLocation;
import com.rayvision.inventory_management.repository.LocationRepository;
import com.rayvision.inventory_management.repository.SupplierLocationRepository;
import com.rayvision.inventory_management.repository.SupplierRepository;
import com.rayvision.inventory_management.service.SupplierLocationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SupplierLocationServiceImpl implements SupplierLocationService {
    private final SupplierLocationRepository supplierLocationRepository;
    private final SupplierRepository supplierRepository;
    private final LocationRepository locationRepository;

    public SupplierLocationServiceImpl(SupplierLocationRepository supplierLocationRepository,
                                       SupplierRepository supplierRepository,
                                       LocationRepository locationRepository) {
        this.supplierLocationRepository = supplierLocationRepository;
        this.supplierRepository = supplierRepository;
        this.locationRepository = locationRepository;
    }

    @Override
    public List<SupplierLocation> getLocationsBySupplier(Long companyId, Long supplierId) {
        // Optionally, verify that the supplier belongs to the given company.
        return supplierLocationRepository.findBySupplierId(supplierId);
    }

    @Override
    public Optional<SupplierLocation> getSupplierLocationById(Long companyId, Long supplierId, Long locationId) {
        // Here we query by supplier and location IDs.
        return supplierLocationRepository.findBySupplierIdAndLocationId(supplierId, locationId);
    }

    @Override
    public SupplierLocation saveSupplierLocation(Long companyId, Long supplierId, SupplierLocation supplierLocation) {
        // Ensure the supplier exists and belongs to the company.
        Supplier supplier = supplierRepository.findByCompanyIdAndId(companyId, supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found for company: " + companyId));
        // Ensure the location exists and belongs to the company.
        // (Assumes LocationRepository has method findByCompanyIdAndId)
        Location location = locationRepository.findByCompanyIdAndId(companyId, supplierLocation.getLocation().getId())
                .orElseThrow(() -> new RuntimeException("Location not found for company: " + companyId));
        supplierLocation.setSupplier(supplier);
        supplierLocation.setLocation(location);
        return supplierLocationRepository.save(supplierLocation);
    }

    @Override
    public void deleteSupplierLocation(Long companyId, Long supplierId, Long locationId) {
        SupplierLocation supplierLocation = supplierLocationRepository.findBySupplierIdAndLocationId(supplierId, locationId)
                .orElseThrow(() -> new RuntimeException("Supplier location not found for supplier: " + supplierId));
        supplierLocationRepository.delete(supplierLocation);
    }

}
