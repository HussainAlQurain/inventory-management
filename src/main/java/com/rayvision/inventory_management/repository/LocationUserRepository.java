package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.LocationUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocationUserRepository extends JpaRepository<LocationUser, Long> {

    Optional<LocationUser> findByLocationIdAndUserId(Long locationId, Long userId);
}
