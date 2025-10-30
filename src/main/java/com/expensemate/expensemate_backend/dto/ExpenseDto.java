package com.expensemate.expensemate_backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ExpenseDto {
    private Long id;
    private Double amount;
    private String category;
    private String description;
    private LocalDate date;
}
