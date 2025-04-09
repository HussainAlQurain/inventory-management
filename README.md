
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