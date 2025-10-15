package com.expensemate.expensemate_backend.service;

import com.expensemate.expensemate_backend.dto.InsightDto;
import com.expensemate.expensemate_backend.model.Budget;
import com.expensemate.expensemate_backend.model.Expense;
import com.expensemate.expensemate_backend.repository.BudgetRepository;
import com.expensemate.expensemate_backend.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.List;

// PDF Generation imports
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;

@Service
public class InsightService {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

    public InsightService(ExpenseRepository expenseRepository,
                          BudgetRepository budgetRepository) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
    }

    public InsightDto generateUserInsights(Long userId, String monthParam) {
        List<Expense> expenses;
        LocalDate start;
        LocalDate end;

        // Determine period
        if (monthParam == null || monthParam.equalsIgnoreCase("all")) {
            expenses = expenseRepository.findByUserId(userId);
        } else if (monthParam.equalsIgnoreCase("current")) {
            YearMonth current = YearMonth.now();
            start = current.atDay(1);
            end = current.atEndOfMonth();
            expenses = expenseRepository.findByUserIdAndDateBetween(userId, start, end);
        } else {
            try {
                YearMonth targetMonth = YearMonth.parse(monthParam);
                start = targetMonth.atDay(1);
                end = targetMonth.atEndOfMonth();
                expenses = expenseRepository.findByUserIdAndDateBetween(userId, start, end);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid month format. Use 'YYYY-MM', 'current', or 'all'.");
            }
        }

        if (expenses.isEmpty()) {
            return new InsightDto(0, 0, "N/A", Collections.emptyMap(),
                    "No spending data available for this period.");
        }

        // --- Total & Daily Average ---
        double totalSpent = expenses.stream().mapToDouble(Expense::getAmount).sum();
        long days = expenses.stream().map(Expense::getDate).distinct().count();
        double dailyAvg = totalSpent / Math.max(days, 1);

        // --- Category Totals & Top Category ---
        Map<String, Double> categoryTotals = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory,
                        Collectors.summingDouble(Expense::getAmount)));

        String topCategory = categoryTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        StringBuilder message = new StringBuilder();
        message.append("🤖 AI-Powered Insights\n\n");

        // --- 1. Top Spending Category ---
        message.append("1️⃣ Top Spending Category: ");
        message.append(String.format("%s (₹%.2f total)\n\n", topCategory, categoryTotals.getOrDefault(topCategory, 0.0)));

        // --- 2. Spending Trend ---
        double previousMonthTotal = getPreviousMonthTotal(userId);
        if (previousMonthTotal > 0) {
            double changePercent = ((totalSpent - previousMonthTotal) / previousMonthTotal) * 100;
            String trend = changePercent >= 0 ? "increased" : "decreased";
            message.append("2️⃣ Spending Trend: ");
            message.append(String.format("Your spending has %s by %.2f%% compared to last month.\n\n",
                    trend, Math.abs(changePercent)));
        }

        // --- 3. Unusual Expenses Alert ---
        double avgExpense = expenses.stream().mapToDouble(Expense::getAmount).average().orElse(0);
        List<Expense> unusual = expenses.stream()
                .filter(e -> e.getAmount() > 2 * avgExpense)
                .collect(Collectors.toList());
        if (!unusual.isEmpty()) {
            message.append("3️⃣ Unusual Expenses Alert: ⚠️ ");
            unusual.forEach(e -> message.append(String.format("%s (₹%.2f), ", e.getCategory(), e.getAmount())));
            message.append("stands out as unusually high.\n\n");
        }

        // --- 4. Budget Usage ---
        YearMonth month = YearMonth.now();
        if (monthParam != null && !monthParam.equalsIgnoreCase("all") && !monthParam.equalsIgnoreCase("current")) {
            month = YearMonth.parse(monthParam);
        }
        Optional<Budget> budgetOpt = budgetRepository.findByUserIdAndMonthAndYear(userId, month.getMonthValue(), month.getYear());
        budgetOpt.ifPresent(budget -> {
            double budgetAmount = budget.getAmount();
            double budgetUsage = (totalSpent / budgetAmount) * 100;
            message.append("4️⃣ Budget Usage: ");
            if (budgetUsage >= 80) {
                message.append("⚠️ ");
            }
            message.append(String.format("You've used %.0f%% of your monthly budget (₹%.2f / ₹%.2f).\n\n",
                    budgetUsage, totalSpent, budgetAmount));
        });

        // --- 5. Daily Average Spending ---
        message.append("5️⃣ Daily Average Spending: ₹");
        message.append(String.format("%.2f per day this month.\n\n", dailyAvg));

        // --- 6. Category Breakdown ---
        message.append("6️⃣ Category Breakdown:\n");
        categoryTotals.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(entry -> {
                    message.append(String.format("   - %s: ₹%.2f\n", entry.getKey(), entry.getValue()));
                });
        message.append("\n");

        // --- 7. Recurring Expense Alert ---
        Map<String, Long> categoryCounts = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory, Collectors.counting()));
        List<String> recurringCategories = categoryCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= 3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (!recurringCategories.isEmpty()) {
            message.append("7️⃣ Recurring Expenses: You consistently spend on: ");
            message.append(String.join(", ", recurringCategories));
            message.append(".\n\n");
        }

        // --- 8. Potential Savings Tip ---
        if (budgetOpt.isPresent()) {
            double budgetAmount = budgetOpt.get().getAmount();
            if (totalSpent > budgetAmount * 0.7) {
                message.append("8️⃣ Potential Savings Tip: Consider reducing spending in ");
                message.append(String.format("%s or other categories to stay under budget.\n\n", topCategory));
            }
        }

        // --- 9. High Spending Days ---
        Map<LocalDate, Double> dailySpending = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getDate,
                        Collectors.summingDouble(Expense::getAmount)));
        Optional<Map.Entry<LocalDate, Double>> highestSpendingDay = dailySpending.entrySet().stream()
                .max(Map.Entry.comparingByValue());
        if (highestSpendingDay.isPresent() && highestSpendingDay.get().getValue() > dailyAvg * 2) {
            message.append("9️⃣ High Spending Days: ⚠️ Highest spending occurred on ");
            message.append(String.format("%s: ₹%.2f.\n\n", highestSpendingDay.get().getKey(), highestSpendingDay.get().getValue()));
        }

        // --- 10. Expense Categorization Suggestion ---
        List<Expense> uncategorized = expenses.stream()
                .filter(e -> e.getCategory() == null || e.getCategory().trim().isEmpty())
                .collect(Collectors.toList());
        if (!uncategorized.isEmpty()) {
            message.append("🔟 Expense Categorization Suggestion: Some uncategorized expenses could be assigned to ");
            message.append(String.format("%s or other categories for better tracking.\n\n", topCategory));
        }

        // --- 11. Alert for Overspending ---
        if (budgetOpt.isPresent()) {
            double budgetAmount = budgetOpt.get().getAmount();
            if (totalSpent > budgetAmount) {
                double overspend = totalSpent - budgetAmount;
                message.append("1️⃣1️⃣ Alert for Overspending: You have exceeded your monthly budget by ₹");
                message.append(String.format("%.2f.\n\n", overspend));
            } else if (totalSpent > budgetAmount * 0.8) {
                double potentialOverspend = totalSpent - budgetAmount;
                message.append("1️⃣1️⃣ Alert for Overspending: At this rate, you may exceed your monthly budget by ₹");
                message.append(String.format("%.2f.\n\n", potentialOverspend));
            }
        }

        // --- 12. Savings Opportunity ---
        if (budgetOpt.isPresent()) {
            double budgetAmount = budgetOpt.get().getAmount();
            if (totalSpent < budgetAmount * 0.5) {
                double potentialSavings = budgetAmount * 0.5 - totalSpent;
                message.append("1️⃣2️⃣ Savings Opportunity: You could save up to ₹");
                message.append(String.format("%.2f by maintaining this spending level.\n\n", potentialSavings));
            }
        }

        // --- Smart Expense Categorization (Auto-categorize) ---
        Map<String, String> keywordMap = Map.of(
                "swiggy", "Food",
                "zomato", "Food",
                "uber", "Travel",
                "ola", "Travel",
                "electricity", "Utilities",
                "rent", "Housing"
        );
        expenses.stream()
                .filter(e -> e.getCategory() == null || e.getCategory().isBlank())
                .forEach(e -> {
                    keywordMap.forEach((key, cat) -> {
                        if (e.getDescription().toLowerCase().contains(key)) {
                            e.setCategory(cat);
                        }
                    });
                });


        return new InsightDto(dailyAvg, totalSpent, topCategory, categoryTotals, message.toString());
    }

    private double getPreviousMonthTotal(Long userId) {
        YearMonth previous = YearMonth.now().minusMonths(1);
        LocalDate start = previous.atDay(1);
        LocalDate end = previous.atEndOfMonth();
        List<Expense> prevExpenses = expenseRepository.findByUserIdAndDateBetween(userId, start, end);
        return prevExpenses.stream().mapToDouble(Expense::getAmount).sum();
    }

    /**
     * Export AI-powered insights as PDF.
     *
     * @param userId User ID for insights generation
     * @param monthParam Month filter parameter
     * @return PDF as byte array
     */
    public byte[] exportInsightsPDF(Long userId, String monthParam) {
        try {
            // Generate insights data
            InsightDto insights = generateUserInsights(userId, monthParam);
            
            // Create PDF document
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);
            
            document.open();
            
            // Add title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("AI-Powered Financial Insights", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Add date
            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.GRAY);
            Paragraph date = new Paragraph("Generated on: " + LocalDate.now().toString(), dateFont);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(30);
            document.add(date);
            
            // Add insights content
            Font contentFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);
            String[] lines = insights.getMessage().split("\n");
            
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    document.add(new Paragraph(" ")); // Add spacing
                } else {
                    Paragraph paragraph = new Paragraph(line, contentFont);
                    paragraph.setSpacingAfter(8);
                    document.add(paragraph);
                }
            }
            
            // Add summary section
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            
            Font summaryFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.DARK_GRAY);
            Paragraph summaryTitle = new Paragraph("Summary", summaryFont);
            summaryTitle.setSpacingAfter(15);
            document.add(summaryTitle);
            
            // Add key metrics
            Font metricFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);
            document.add(new Paragraph("Monthly Total: ₹" + String.format("%.2f", insights.getMonthlyTotal()), metricFont));
            document.add(new Paragraph("Daily Average: ₹" + String.format("%.2f", insights.getDailyAverage()), metricFont));
            document.add(new Paragraph("Top Category: " + insights.getTopCategory(), metricFont));
            
            document.close();
            
            return outputStream.toByteArray();
            
        } catch (DocumentException e) {
            System.err.println("Error generating PDF: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}
