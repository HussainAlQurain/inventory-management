package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    UserRepository userRepository;

    public Users createUser(Users user) {
        user.setPassword();
        return userRepository.save(user);
    }
}
