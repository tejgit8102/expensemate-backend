package com.expensemate.expensemate_backend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsightDto {
    private double dailyAverage;
    private double monthlyTotal;
    private String topCategory;
    private Map<String, Double> categoryBreakdown; // e.g. {"Food":1200, "Travel":800}
    private String message; // “You’ve spent 25% more than last month on Food.”
}

