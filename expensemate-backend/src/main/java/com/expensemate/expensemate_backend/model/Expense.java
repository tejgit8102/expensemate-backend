package com.expensemate.expensemate_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;

    private String category;

    private String description;

    private LocalDate date;

    // ✅ New field: allows admin to flag suspicious expenses
    @Column(nullable = false)
    private boolean flagged = false;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
