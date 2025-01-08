
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