package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.PurchaseOption;
import com.rayvision.inventory_management.model.PurchaseOptionPriceHistory;
import com.rayvision.inventory_management.repository.PurchaseOptionPriceHistoryRepository;
import com.rayvision.inventory_management.repository.PurchaseOptionRepository;
import com.rayvision.inventory_management.service.PurchaseOptionPriceHistoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Transactional
public class PurchaseOptionPriceHistoryServiceImpl implements PurchaseOptionPriceHistoryService {

    private final PurchaseOptionPriceHistoryRepository historyRepository;
    private final PurchaseOptionRepository purchaseOptionRepository;

    public PurchaseOptionPriceHistoryServiceImpl(
            PurchaseOptionPriceHistoryRepository historyRepository,
            PurchaseOptionRepository purchaseOptionRepository
    ) {
        this.historyRepository = historyRepository;
        this.purchaseOptionRepository = purchaseOptionRepository;
    }

    @Override
    public void recordPriceChange(PurchaseOption po, Double newPrice) {
        // 1) Create a new history record
        PurchaseOptionPriceHistory hist = new PurchaseOptionPriceHistory();
        hist.setPurchaseOption(po);
        hist.setPrice(newPrice);
        hist.setTimestamp(LocalDateTime.now());

        // 2) Save the history
        historyRepository.save(hist);

        // 3) Update the PurchaseOption
        po.setPrice(newPrice);
        purchaseOptionRepository.save(po);
    }
}
