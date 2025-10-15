package com.expensemate.expensemate_backend.service;

import com.expensemate.expensemate_backend.model.User;
import com.expensemate.expensemate_backend.repository.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("Email cannot be null or blank");
        }

        // ✅ Normalize email for case-insensitive lookup
        String normalizedEmail = email.toLowerCase();

        // ✅ Fetch user or throw if not found
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalizedEmail));

        // ✅ Check if user is deactivated
        if (!user.isActive()) {
            throw new DisabledException("User account is deactivated. Please contact admin.");
        }

        // ✅ Debug info
        System.out.println("Authenticated user: " + normalizedEmail + " with role: " + user.getRole());

        // ✅ Return UserDetails wrapper
        return new UserDetailsImpl(user);
    }
}
