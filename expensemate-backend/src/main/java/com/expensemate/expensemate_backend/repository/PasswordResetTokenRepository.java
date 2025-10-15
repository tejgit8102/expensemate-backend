package com.expensemate.expensemate_backend.repository;


import com.expensemate.expensemate_backend.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByEmail(String email);
    Optional<PasswordResetToken> findByEmailAndOtp(String email, String otp);
    void deleteByEmail(String email); // clear old OTPs when generating new one
}

