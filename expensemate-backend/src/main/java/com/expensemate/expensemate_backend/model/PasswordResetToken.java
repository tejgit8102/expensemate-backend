package com.expensemate.expensemate_backend.model;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String otp;
    private Long expirationTime; // in millis
}

