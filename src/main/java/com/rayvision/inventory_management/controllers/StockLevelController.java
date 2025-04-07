package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.dto.StockLevelDTO;
import com.rayvision.inventory_management.service.StockTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/stock-levels")
public class StockLevelController {
    private final StockTransactionService stockTxService;

    public StockLevelController(StockTransactionService stockTxService) {
        this.stockTxService = stockTxService;
    }

    /**
     * GET /stock-levels/location/{locationId}
     *   ?startDate=YYYY-MM-DD
     *   &endDate=YYYY-MM-DD
     */
    @GetMapping("/location/{locationId}")
    public ResponseEntity<List<StockLevelDTO>> getStockForLocation(
            @PathVariable Long locationId,
            @RequestParam(required=false) String startDate,
            @RequestParam(required=false) String endDate
    ) {
        LocalDate start = (startDate != null) ? LocalDate.parse(startDate) : null;
        LocalDate end   = (endDate   != null) ? LocalDate.parse(endDate)   : null;

        List<StockLevelDTO> stockLevels =
                stockTxService.getStockLevelsForLocation(locationId, start, end);

        return ResponseEntity.ok(stockLevels);
    }

}
