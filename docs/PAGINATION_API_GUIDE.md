# Pagination API Guide for Inventory Management System

This document provides guidelines for using the paginated API endpoints in our inventory management system. These endpoints allow more efficient data retrieval by fetching only the required records instead of loading everything at once.

## Table of Contents
1. [General Pagination Structure](#general-pagination-structure)
2. [Inventory Items Pagination](#inventory-items-pagination)
3. [Suppliers Pagination](#suppliers-pagination)
4. [Purchase Orders Pagination](#purchase-orders-pagination)

## General Pagination Structure

### Request Parameters

All paginated endpoints accept these common parameters:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | Integer | 0 | Zero-based page number (first page is 0) |
| `size` | Integer | 10 | Number of records per page |
| `sort` | String | Varies | Format: `"property,direction"` where direction is `asc` or `desc` |

### Response Structure

All paginated endpoints return a common response structure:

```json
{
  "content": [
    // Array of items for the current page
  ],
  "totalElements": 42,   // Total count of all matching records
  "totalPages": 5,       // Total number of pages
  "currentPage": 0,      // Current page number (zero-based)
  "pageSize": 10,        // Number of records per page 
  "hasNext": true,       // Whether there are more pages after this one
  "hasPrevious": false   // Whether there are pages before this one
}
```

## Inventory Items Pagination

### Endpoint
```
GET /companies/{companyId}/inventory-items/paginated
```

### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `companyId` | Long | Yes | The ID of the company |
| `page` | Integer | No | Page number (default: 0) |
| `size` | Integer | No | Items per page (default: 10) |
| `sort` | String | No | Sort specification (default: "name,asc") |
| `categoryId` | Long | No | Filter by category ID |
| `searchTerm` | String | No | Search in names and descriptions |
| `includeDetails` | Boolean | No | Include additional details (default: false) |

### Example Request

```
GET /companies/1/inventory-items/paginated?page=0&size=5&sort=name,asc&searchTerm=chicken
```

### Example Response

```json
{
  "content": [
    {
      "id": 12,
      "name": "Chicken Breast",
      "code": "CB001",
      "description": "Fresh boneless chicken breast",
      "isCountable": true,
      "currentPrice": 5.99,
      "categoryName": "Meat",
      "inventoryUom": {
        "id": 3,
        "name": "Kilogram",
        "abbreviation": "kg"
      },
      "countUom": {
        "id": 1,
        "name": "Each",
        "abbreviation": "ea"
      }
    },
    {
      "id": 14,
      "name": "Chicken Thighs",
      "code": "CT002",
      "description": "Boneless chicken thighs",
      "isCountable": true,
      "currentPrice": 4.50,
      "categoryName": "Meat",
      "inventoryUom": {
        "id": 3,
        "name": "Kilogram",
        "abbreviation": "kg"
      },
      "countUom": {
        "id": 1,
        "name": "Each",
        "abbreviation": "ea"
      }
    }
    // Additional items...
  ],
  "totalElements": 12,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 5,
  "hasNext": true,
  "hasPrevious": false
}
```

## Suppliers Pagination

### Endpoint
```
GET /companies/{companyId}/suppliers/paginated
```

### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `companyId` | Long | Yes | The ID of the company |
| `page` | Integer | No | Page number (default: 0) |
| `size` | Integer | No | Suppliers per page (default: 10) |
| `sort` | String | No | Sort specification (default: "name,asc") |
| `searchTerm` | String | No | Search in names, codes, and contact info |

### Example Request

```
GET /companies/1/suppliers/paginated?page=0&size=5&searchTerm=foods
```

### Example Response

```json
{
  "content": [
    {
      "id": 3,
      "name": "Global Foods Inc.",
      "code": "GFI",
      "contactPerson": "John Smith",
      "email": "orders@globalfoods.com",
      "phone": "+1-555-123-4567",
      "address": "123 Supply St",
      "city": "Foodville",
      "state": "CA", 
      "zipCode": "90210"
    },
    {
      "id": 7,
      "name": "Premium Foods Co.",
      "code": "PFC",
      "contactPerson": "Sarah Johnson",
      "email": "orders@premiumfoods.com",
      "phone": "+1-555-987-6543",
      "address": "456 Quality Ave",
      "city": "Gourmet City",
      "state": "NY",
      "zipCode": "10001"
    }
    // Additional suppliers...
  ],
  "totalElements": 8,
  "totalPages": 2,
  "currentPage": 0,
  "pageSize": 5,
  "hasNext": true,
  "hasPrevious": false
}
```

## Purchase Orders Pagination

### Endpoint
```
GET /companies/{companyId}/purchase-orders/paginated
```

### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `companyId` | Long | Yes | The ID of the company |
| `page` | Integer | No | Page number (default: 0) |
| `size` | Integer | No | Orders per page (default: 10) |
| `sort` | String | No | Sort specification (default: "creationDate,desc") |
| `startDate` | ISO Date | No | Filter by creation date from (inclusive) |
| `endDate` | ISO Date | No | Filter by creation date to (inclusive) |
| `supplierId` | Long | No | Filter by supplier ID |
| `locationId` | Long | No | Filter by location ID |
| `status` | String | No | Filter by order status (DRAFT, SENT, DELIVERED, COMPLETED) |

### Example Request

```
GET /companies/1/purchase-orders/paginated?page=0&size=5&startDate=2025-03-01&status=SENT&locationId=1
```

### Example Response

```json
{
  "content": [
    {
      "id": 117,
      "orderNumber": "PO-1650912842242",
      "sentDate": "2025-04-12",
      "deliveryDate": null,
      "status": "SENT",
      "comments": "Monthly stock replenishment",
      "buyerLocationName": "Main Warehouse",
      "supplierName": "Global Foods Inc.",
      "createdByUserId": 1,
      "createdByUserName": "admin",
      "total": 1245.67
    },
    {
      "id": 114,
      "orderNumber": "PO-1650850603412",
      "sentDate": "2025-04-10",
      "deliveryDate": null,
      "status": "SENT",
      "comments": "Urgent stock refill",
      "buyerLocationName": "Main Warehouse",
      "supplierName": "Premium Produce LLC",
      "createdByUserId": 1,
      "createdByUserName": "admin",
      "total": 532.40
    }
    // Additional orders...
  ],
  "totalElements": 17,
  "totalPages": 4,
  "currentPage": 0,
  "pageSize": 5,
  "hasNext": true,
  "hasPrevious": false
}
```

## Implementation Notes for Frontend Developers

### Best Practices

1. **Default Page Size**: Consider using a reasonable default page size (10-25 items) to balance performance and usability.

2. **Infinite Scrolling**: For long lists, consider implementing infinite scrolling using the `hasNext` property to determine when to load more data.

3. **Sorting Indicators**: When implementing sortable columns, show clear indicators of the current sort direction.

4. **Filtering State**: Preserve filter state when navigating between pages.

5. **Loading States**: Show loading indicators when fetching new pages to improve user experience.

### Sample Code (JavaScript/TypeScript with Axios)

```javascript
// Example using Axios to fetch paginated inventory items
async function fetchInventoryItems(companyId, page = 0, size = 10, sort = "name,asc", filters = {}) {
  try {
    const { categoryId, searchTerm, includeDetails } = filters;
    
    const response = await axios.get(`/companies/${companyId}/inventory-items/paginated`, {
      params: {
        page,
        size,
        sort,
        categoryId,
        searchTerm,
        includeDetails
      }
    });
    
    return response.data;
  } catch (error) {
    console.error('Error fetching inventory items:', error);
    throw error;
  }
}

// Usage example
async function loadInventoryPage() {
  const companyId = 1;
  const currentPage = 0;
  const pageSize = 10;
  
  try {
    const result = await fetchInventoryItems(companyId, currentPage, pageSize, "name,asc", {
      searchTerm: "chicken"
    });
    
    // Update UI with the items
    displayItems(result.content);
    
    // Update pagination controls
    updatePaginationControls({
      currentPage: result.currentPage,
      totalPages: result.totalPages,
      hasNext: result.hasNext,
      hasPrevious: result.hasPrevious
    });
    
  } catch (error) {
    showErrorMessage("Failed to load inventory items");
  }
}
```