package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.UserPrincipal;
import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Users> user =  userRepository.findByUsername(username);
        if(user.isEmpty()){
            throw new UsernameNotFoundException("user not found");
        }

        return new UserPrincipal(user.get());
    }
}
