package com.expensemate.expensemate_backend.controller;

import com.expensemate.expensemate_backend.dto.ChangePasswordRequest;
import com.expensemate.expensemate_backend.dto.JwtResponse;
import com.expensemate.expensemate_backend.dto.LoginRequest;
import com.expensemate.expensemate_backend.dto.SignupRequest;
import com.expensemate.expensemate_backend.model.Role;
import com.expensemate.expensemate_backend.model.User;
import com.expensemate.expensemate_backend.repository.UserRepository;
import com.expensemate.expensemate_backend.security.JwtUtil;
import com.expensemate.expensemate_backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public AuthController(AuthenticationManager authManager,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          UserService userService) {
        this.authManager = authManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    // -------------------- REGISTER --------------------
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SignupRequest request) {
        String email = request.getEmail().toLowerCase();

        if (Boolean.TRUE.equals(userRepository.existsByUsername(request.getUsername()))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is already taken!"));
        }

        if (Boolean.TRUE.equals(userRepository.existsByEmail(email))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is already in use!"));
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .active(true) // <-- Ensure new users are active by default
                .build();

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
    }

    // -------------------- LOGIN --------------------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String email = request.getEmail().toLowerCase();

        // Check if user exists first
        if (!userRepository.existsByEmail(email)) {
            return ResponseEntity.status(401).body("Invalid credentials. Please check your email and password.");
        }

        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid credentials. Please check your email and password.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name(), user.getUsername());

        return ResponseEntity.ok(new JwtResponse(token));
    }

    // -------------------- FORGOT PASSWORD --------------------
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email").toLowerCase();

        if (Boolean.FALSE.equals(userRepository.existsByEmail(email))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email not found!"));
        }

        userService.generateOtpForUser(email);

        return ResponseEntity.ok(Map.of("message", "OTP sent to email."));
    }

    // -------------------- RESET PASSWORD --------------------
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email").toLowerCase();
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        boolean isValid = userService.verifyOtp(email, otp);
        if (!isValid) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired OTP!"));
        }

        // Check if new password is different from current password
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be different from your current password!"));
        }

        userService.updatePassword(email, newPassword);

        return ResponseEntity.ok(Map.of("message", "Password reset successfully!"));
    }

    // -------------------- GET USER PROFILE ----------------------
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extract user ID from JWT token
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            Long userId = jwtUtil.extractUserId(token);
            
            if (userId == null) {
                return ResponseEntity.status(401).body("Invalid token");
            }
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Return user profile data
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", user.getId());
            profile.put("username", user.getUsername());
            profile.put("email", user.getEmail());
            profile.put("role", user.getRole().name());
            
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid token or user not found");
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updateData, 
                                          @RequestHeader("Authorization") String authHeader) {
        try {
            System.out.println("Profile update request received");
            System.out.println("Update data: " + updateData);
            System.out.println("Auth header: " + authHeader);
            
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String email = jwtUtil.extractUsername(token);
            System.out.println("Extracted email: " + email);
            
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            System.out.println("User found: " + user.getEmail() + ", current username: " + user.getUsername());
            
            // Update username if provided
            if (updateData.containsKey("username")) {
                String newUsername = updateData.get("username");
                System.out.println("New username: " + newUsername);
                if (newUsername != null && !newUsername.trim().isEmpty()) {
                    // Check if username is already taken by another user
                    boolean usernameExists = userRepository.existsByUsernameAndIdNot(newUsername, user.getId());
                    System.out.println("Username exists for other user: " + usernameExists);
                    if (usernameExists) {
                        return ResponseEntity.badRequest().body("Username already taken");
                    }
                    user.setUsername(newUsername);
                    System.out.println("Username updated to: " + newUsername);
                }
            }
            
            userRepository.save(user);
            System.out.println("Profile updated successfully");
            
            return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
        } catch (Exception e) {
            System.out.println("Error in profile update: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(401).body("Invalid token or user not found");
        }
    }

    // -------------------- CHANGE PASSWORD --------------------
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request,
                                           @RequestHeader("Authorization") String authHeader) {
        try {
            System.out.println("Change password request received");
            System.out.println("Auth header: " + authHeader);
            
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String email = jwtUtil.extractUsername(token);
            System.out.println("Extracted email: " + email);
            
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            System.out.println("User found: " + user.getEmail());
            
            // Validate current password
            boolean currentPasswordMatches = passwordEncoder.matches(request.getCurrentPassword(), user.getPassword());
            System.out.println("Current password matches: " + currentPasswordMatches);
            
            if (!currentPasswordMatches) {
                return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
            }
            
            // Check if new password is different from current password
            if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("error", "New password must be different from your current password"));
            }
            
            // Validate password confirmation
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(Map.of("error", "New password and confirmation do not match"));
            }
            
            // Update password
            userService.updatePassword(email, request.getNewPassword());
            System.out.println("Password updated successfully");
            
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            System.out.println("Error in change password: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(401).body("Invalid token or user not found");
        }
    }
}
