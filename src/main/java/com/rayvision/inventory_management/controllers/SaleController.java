package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.Sale;
import com.rayvision.inventory_management.model.SaleLine;
import com.rayvision.inventory_management.model.dto.SaleCreateDTO;
import com.rayvision.inventory_management.model.dto.SaleLineResponseDTO;
import com.rayvision.inventory_management.model.dto.SaleResponseDTO;
import com.rayvision.inventory_management.model.dto.SaleSummaryDTO;
import com.rayvision.inventory_management.service.SaleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/sales")
public class SaleController {
    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    // 1) POST a new sale from POS or front end
    @PostMapping
    public ResponseEntity<SaleResponseDTO> createSale(@RequestBody SaleCreateDTO dto) {
        Sale saved = saleService.createSale(dto);
        SaleResponseDTO resp = toSaleResponseDTO(saved);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    // 2) GET all sales in date range by location
    @GetMapping("/location/{locationId}")
    public ResponseEntity<List<SaleSummaryDTO>> getSalesForLocation(
            @PathVariable Long locationId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end
    ) {
        LocalDateTime startLDT = (start != null)
                ? LocalDateTime.parse(start)
                : LocalDateTime.of(1970,1,1,0,0);
        LocalDateTime endLDT = (end != null)
                ? LocalDateTime.parse(end)
                : LocalDateTime.of(3000,1,1,23,59);

        List<Sale> sales = saleService.findSalesByLocationAndDateRange(locationId, startLDT, endLDT);
        List<SaleSummaryDTO> dtos = sales.stream()
                .map(this::toSaleSummaryDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // 3) GET sale detail
    @GetMapping("/{saleId}")
    public ResponseEntity<SaleResponseDTO> getSaleDetails(@PathVariable Long saleId) {
        return saleService.getSaleById(saleId)
                .map(this::toSaleResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 4) If you want to handle updates or deletions, you can define them here
    // ...

    // 5) Example mapping to a response DTO
    private SaleResponseDTO toSaleResponseDTO(Sale sale) {
        SaleResponseDTO dto = new SaleResponseDTO();
        dto.setSaleId(sale.getId());
        dto.setLocationId(sale.getLocation().getId());
        dto.setSaleDateTime(sale.getSaleDateTime());
        dto.setPosReference(sale.getPosReference());
        dto.setTotalRevenue(sale.getTotalRevenue());
        dto.setTotalCost(sale.getTotalCost());
        dto.setTotalProfit(sale.getTotalProfit());

        // lines
        List<SaleLineResponseDTO> lineDtos = new ArrayList<>();
        for (SaleLine line : sale.getLines()) {
            SaleLineResponseDTO ld = new SaleLineResponseDTO();
            ld.setSaleLineId(line.getId());
            if (line.getMenuItem() != null) {
                ld.setMenuItemId(line.getMenuItem().getId());
                ld.setMenuItemName(line.getMenuItem().getName());
                ld.setPosCode(line.getMenuItem().getPosCode());
            } else {
                ld.setPosCode(line.getPosCode());
            }
            ld.setQuantity(line.getQuantity());
            ld.setUnitPriceAtSale(line.getUnitPriceAtSale());
            ld.setExtendedPrice(line.getExtendedPrice());
            ld.setCostAtSaleTime(line.getCostAtSaleTime());
            ld.setProfitAtSaleTime(line.getProfitAtSaleTime());
            lineDtos.add(ld);
        }
        dto.setLines(lineDtos);
        return dto;
    }

    private SaleSummaryDTO toSaleSummaryDTO(Sale sale) {
        // a smaller version
        SaleSummaryDTO dto = new SaleSummaryDTO();
        dto.setSaleId(sale.getId());
        dto.setSaleDateTime(sale.getSaleDateTime());
        dto.setLocationId(sale.getLocation().getId());
        dto.setPosReference(sale.getPosReference());
        dto.setTotalRevenue(sale.getTotalRevenue());
        dto.setTotalCost(sale.getTotalCost());
        dto.setTotalProfit(sale.getTotalProfit());
        return dto;
    }
}
