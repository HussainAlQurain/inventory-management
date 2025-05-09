package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUsername(String username);
    
    /**
     * Find users by company ID with pagination and optional search term
     * @param companyId Company ID
     * @param searchTerm Optional search term for filtering users (by name, email, or username)
     * @param pageable Pagination parameters
     * @return Page of matching users
     */
    @Query("""
        SELECT u FROM Users u
        JOIN CompanyUser cu ON u.id = cu.user.id 
        WHERE cu.company.id = :companyId
        AND (COALESCE(:searchTerm, '') = '' OR 
             LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
             LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
             LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
             LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """)
    Page<Users> findByCompanyIdWithSearch(
        @Param("companyId") Long companyId,
        @Param("searchTerm") String searchTerm,
        Pageable pageable
    );
}
