package com.expensemate.expensemate_backend.controller;

import com.expensemate.expensemate_backend.model.User;
import com.expensemate.expensemate_backend.repository.UserRepository;
import com.expensemate.expensemate_backend.service.UserService;
import com.expensemate.expensemate_backend.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, UserRepository userRepository, JwtUtil jwtUtil) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    // Get user profile
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        return userService.getUserByEmail(email)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body("User not found"));
    }

    // Update user profile
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                           @RequestBody User updatedUser) {
        String email = userDetails.getUsername();

        // Check if updated email is already taken by another user
        if (!email.equalsIgnoreCase(updatedUser.getEmail())
                && Boolean.TRUE.equals(userRepository.existsByEmail(updatedUser.getEmail().toLowerCase()))) {
            return ResponseEntity.badRequest().body("Email is already in use by another account");
        }

        return userService.updateProfile(email, updatedUser)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body("User not found"));
    }
}
