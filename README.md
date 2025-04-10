
## Company Controller:
#### What We Have:
- CRUD operations for the Company entity (create, read, update, delete).
- User association management (add and remove users from companies).
- Find companies by user (retrieve companies that a specific user is associated with).
- Location management: You mentioned you might allow for location creation inside a company.

#### Consideration: 
- change the deletion to soft deletion (make it inactive rather than fully removing it.)
- Add validation for email, name etc..
- Implement paginatino and search functionalities:
```Java
@GetMapping
public Page<Company> getCompanies(Pageable pageable) {
    return companyRepository.findAll(pageable);
}
```
- Audit fields:
```Java
@CreatedDate
private LocalDateTime createdDate;

@LastModifiedDate
private LocalDateTime lastModifiedDate;
```
- Add error handling
- Retrieve all locations for a company
- Use DTOs instead of entities

### Unit of measures bases
- KG
- Liter
- Each
- Meter
## IMPORTANT, currently there is no base units and it might cause error calculating the cost. MAR 27 updated the cost calculation to use uom. Reversed this change


### Ordering
- Can order by filling to par for each supplier (which will add all items below par level to the carts)
- Create a cart for each different supplier by selecting items and their quantities.
- Send order with delivery date and sent date.
- if user is not approver user, it will create order pending for approval. if there is approval list for the location based on predefined selected users to approve.
- order list will display orders placed, orders pending and will allow to receive orders.
- receiving orders will allow to change update the price, or create credit memo for one time extra charge for the price, update quantities to be received, and also update the price for the item fully so that next time it will be directly used.
- User will also be able to receive invoices without orders, in case they didn't enter an order before.

## Ordering & Purchasing (Food & Beverage Inventory System)

### Basic Concepts
1. **Order** represents a purchase order from a Location to a Supplier.
2. **Approval Cycles**: Certain Locations may have an approval workflow that requires one or more users to approve an order before it is finally “sent” to the Supplier.
3. **Carts**: Before finalizing an Order, the user can add items to a “cart” for a specific Supplier and modify quantities, remove items, etc.

### Workflow & Features
1. **Order Creation** (Draft / Cart Stage)
    - A user can “Fill to PAR” for each Supplier.
        - The system calculates `(PAR level - current on-hand)` for each item that belongs to a particular Supplier, generating lines in a “cart.”
        - The user can then adjust quantities, remove lines, or add new items.
        - One cart (draft order) is generated per Supplier.
    - Alternatively, a user can manually create a cart for a chosen Supplier and specify item lines & quantities.

2. **Approval Process** (Optional)
    - Each Location may or may not have an approval cycle.
    - If the Location has an approval cycle, once the user “submits” the order, it goes into `SUBMITTED_FOR_APPROVAL` status.
    - Approvers (assigned to that Location’s cycle) can approve or reject.
    - Once the final approver okays the order, the order status changes to `APPROVED`, and is ready to “send” to the Supplier.
    - If the Location has **no** approval cycle, once the user “submits,” the order can be sent immediately.

3. **Sending an Order**
    - When the order is ready (either no approvals needed or fully approved):
        - The system sets the status to `SENT`.
        - An email is sent to the Supplier’s email address (or optional SMS / printing, though not implemented now).
        - The `sentDate` is recorded.

4. **Receiving an Order**
    - On or after the `deliveryDate`, the user can mark items as received.
    - The system can automatically list “pending deliveries” for the day. (Likely a front-end filter or scheduling approach.)
    - During receiving:
        1. User can **override** the price if the invoice says a different price.
            - This triggers a **moving average** cost update on the InventoryItem (or last purchase price, etc.).
        2. User can adjust the final quantity received if it differs from the ordered quantity (over‐delivery, short‐delivery).
        3. (Optional) The user can add “credit memos” or surcharges (delivery fee, etc.) that can factor into the final cost.
    - Upon finalizing the receive, the system posts stock transactions (`recordPurchase`) to update on-hand quantity in the location.

5. **Receiving Invoices Without Prior Orders**
    - Sometimes the user never created an order.
    - The user can still “Receive Inventory” by creating an “invoice” record that references items, quantity, and price.
    - This also updates stock on-hand and item cost.

6. **Order Statuses**
    - `DRAFT`: The order/carts are being built, not yet submitted nor approved.
    - `SUBMITTED_FOR_APPROVAL`: Waits for approval from designated users.
    - `APPROVED`: All approvals completed, ready to send.
    - `SENT`: The order was sent to the Supplier (email, etc.).
    - `DELIVERED`: The Supplier has delivered the goods (or partial).
    - `COMPLETED`: The order was fully received and closed out.
    - `CANCELLED`: The order was cancelled.

### Implementation Notes
- An approval cycle is per-Location, with a list of user IDs (or roles) in order. Each must “approve” in turn.
- A cart can be a simple domain object or just a DRAFT order with lines that the user manipulates until submitted.
- “Fill to PAR” logic calculates `(PAR - onHand)` for items at that location. Summaries for each Supplier become separate draft orders (one per Supplier).
- During receiving, we must carefully handle cost updates (moving average or last purchase price).
- Email sending can be done using Spring’s `JavaMailSender`, triggered when order transitions to `SENT`.

