package com.rayvision.inventory_management.util;

import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class SystemUserResolver {

    private final UserRepository userRepository;
    private volatile Long cachedId;          // simple memo-cache

    public SystemUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Guaranteed to throw clear RuntimeException if "system-user" does not exist. */
    public Long getSystemUserId() {
        Long id = cachedId;
        if (id != null) return id;           // cached path

        Users u = userRepository.findByUsername("system-user")
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Username 'system-user' not found – check DataInitializer"));
        cachedId = u.getId();
        return cachedId;
    }
}