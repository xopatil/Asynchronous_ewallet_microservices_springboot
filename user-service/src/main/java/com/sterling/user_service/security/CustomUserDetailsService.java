package com.sterling.user_service.security;

import com.sterling.user_service.model.User;
import com.sterling.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

// UserDetailsService is a Spring Security interface with one method: loadUserByUsername()
// When AuthenticationManager.authenticate() is called during login,
// it internally calls loadUserByUsername() to fetch the user from DB,
// then compares the password. You MUST implement this.
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    // Spring Security calls this automatically during authentication.
    // Return a UserDetails object — Spring Security's representation of a user.
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // Find user in database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + username)
                );

        // Convert our User entity into Spring Security's UserDetails format.
        // org.springframework.security.core.userdetails.User (Spring's built-in class)
        // takes: (username, hashedPassword, listOfRoles)
        // SimpleGrantedAuthority wraps the role string ("ROLE_USER")
        // into a format Spring Security understands.
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(
                        new SimpleGrantedAuthority(user.getRole())
                )
        );
    }
}