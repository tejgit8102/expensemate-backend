package com.expensemate.expensemate_backend.dto;

import lombok.Data;

@Data
public class BudgetDto {
    private Double amount;
    private Integer month;  
    private Integer year;   

    // New fields
    private Double totalSpent;
    private Double remaining;
    private Double percentageUsed;
}
