package com.expensemate.expensemate_backend.service;

import com.expensemate.expensemate_backend.model.User;
import com.expensemate.expensemate_backend.model.PasswordResetToken;
import com.expensemate.expensemate_backend.repository.UserRepository;
import com.expensemate.expensemate_backend.repository.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       PasswordResetTokenRepository tokenRepository,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.eventPublisher = eventPublisher;
    }

    // Fetch user by email (from JWT)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase());
    }

    // Update user profile
    @Transactional
    public Optional<User> updateProfile(String email, User updatedUser) {
        return userRepository.findByEmail(email.toLowerCase())
                .map(user -> {
                    user.setUsername(updatedUser.getUsername());
                    user.setEmail(updatedUser.getEmail().toLowerCase());

                    if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                        user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                    }

                    return userRepository.save(user);
                });
    }

    // Generate OTP for password reset
    @Transactional
    public void generateOtpForUser(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // remove old token if exists
        tokenRepository.deleteByEmail(user.getEmail());

        PasswordResetToken token = new PasswordResetToken();
        token.setEmail(user.getEmail());
        token.setOtp(otp);
        token.setExpirationTime(Instant.now().plusSeconds(600).toEpochMilli()); // valid for 10 mins

        tokenRepository.save(token);

        // Publish an event to send email after transaction commits
        eventPublisher.publishEvent(new OtpGeneratedEvent(this, user.getEmail(), otp));
    }

    // Verify OTP
    @Transactional
    public boolean verifyOtp(String email, String otp) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByEmailAndOtp(email.toLowerCase(), otp);
        if (tokenOpt.isEmpty()) return false;

        PasswordResetToken token = tokenOpt.get();

        if (Instant.now().toEpochMilli() > token.getExpirationTime()) {
            tokenRepository.delete(token); // cleanup expired token
            return false;
        }

        return true;
    }

    // Reset password after OTP verification
    @Transactional
    public void updatePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // cleanup all OTPs for this user
        tokenRepository.deleteByEmail(user.getEmail());
    }
}
