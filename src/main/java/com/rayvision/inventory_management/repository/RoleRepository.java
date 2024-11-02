package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.enums.userRoles;
import com.rayvision.inventory_management.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(userRoles name);
}
