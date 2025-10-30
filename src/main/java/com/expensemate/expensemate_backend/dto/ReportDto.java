package com.expensemate.expensemate_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportDto {
    private Integer month;                        // Month of the report (nullable for annual)
    private Integer year;                         // Year of the report
    private Double totalSpent;                    // Total expenses
    private Double budget;                        // Total budget
    private Double remaining;                     // Remaining budget
    private Map<String, Double> categoryExpenses; // Category-wise expenses, e.g. {Food: 2000, Travel: 800}
    private Map<String, Double> monthlyExpenses; // Monthly expenses for annual reports, e.g. {"January": 1500, "February": 2000}
}
