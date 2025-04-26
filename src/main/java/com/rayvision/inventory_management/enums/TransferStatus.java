package com.rayvision.inventory_management.enums;

/**
 * Status values for inventory transfers between locations
 */
public enum TransferStatus {
    DRAFT,              // lines can still change
    SENT,               // dispatcher clicked "Send"
    RECEIVED,           // receiver confirmed receipt
    COMPLETED,          // stock postings done
    CANCELLED
}