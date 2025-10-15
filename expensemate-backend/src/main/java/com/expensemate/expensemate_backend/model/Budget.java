package com.expensemate.expensemate_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
    name = "budgets",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "month", "year"})
    }
)
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer month;
    private Integer year;
    private Double amount;

    // Track how much has been spent for this budget
    private Double spent = 0.0;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
