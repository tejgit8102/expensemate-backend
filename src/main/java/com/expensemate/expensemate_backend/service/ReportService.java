package com.expensemate.expensemate_backend.service;

import com.expensemate.expensemate_backend.dto.ReportDto;
import com.expensemate.expensemate_backend.model.Expense;
import com.expensemate.expensemate_backend.repository.ExpenseRepository;
import com.expensemate.expensemate_backend.repository.UserRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final BudgetService budgetService;
    private final NotificationService notificationService;

    public ReportService(ExpenseRepository expenseRepository,
                         UserRepository userRepository,
                         BudgetService budgetService,
                         NotificationService notificationService) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.budgetService = budgetService;
        this.notificationService = notificationService;
    }

    // Fetch monthly report
    public ReportDto getMonthlyReport(Long userId, Integer month, Integer year) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(userId, start, end);

        Map<String, Double> categoryExpenses = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.summingDouble(Expense::getAmount)
                ));

        double totalSpent = expenses.stream().mapToDouble(Expense::getAmount).sum();
        double budget = getUserBudget(userId, month, year);
        double remaining = budget - totalSpent;

        return new ReportDto(month, year, totalSpent, budget, remaining, categoryExpenses, null);
    }

    // Fetch annual report
    public ReportDto getAnnualReport(Long userId, Integer year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(userId, start, end);

        Map<String, Double> categoryExpenses = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.summingDouble(Expense::getAmount)
                ));

        // Calculate monthly expenses breakdown - only include months with spending
        Map<String, Double> monthlyExpenses = new LinkedHashMap<>(); // Use LinkedHashMap to maintain order
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                              "July", "August", "September", "October", "November", "December"};
        
        for (int m = 1; m <= 12; m++) {
            final int month = m;
            double monthTotal = expenses.stream()
                    .filter(expense -> expense.getDate().getMonthValue() == month)
                    .mapToDouble(Expense::getAmount)
                    .sum();
            // Only add months that have spending (value > 0)
            if (monthTotal > 0) {
                monthlyExpenses.put(monthNames[m-1], monthTotal);
            }
        }

        double totalSpent = expenses.stream().mapToDouble(Expense::getAmount).sum();

        // ✅ For annual reports, show current month's budget and remaining instead of annual totals
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();
        
        // Use current month's budget if we're in the same year, otherwise use the report year's current month
        int budgetMonth = (currentYear == year) ? currentMonth : currentMonth;
        int budgetYear = (currentYear == year) ? currentYear : year;
        
        double budget = getUserBudget(userId, budgetMonth, budgetYear);
        
        // Calculate current month's spending for remaining calculation
        double currentMonthSpent = expenses.stream()
                .filter(expense -> expense.getDate().getMonthValue() == budgetMonth)
                .mapToDouble(Expense::getAmount)
                .sum();
        
        double remaining = budget - currentMonthSpent;

        return new ReportDto(null, year, totalSpent, budget, remaining, categoryExpenses, monthlyExpenses);
    }

    // Helper: get budget for a specific month/year
    private double getUserBudget(Long userId, Integer month, Integer year) {
        return budgetService.getBudgetStatus(userId, month, year).getAmount();
    }

    // Helper: get month name from month number
    private String getMonthName(Integer month) {
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                              "July", "August", "September", "October", "November", "December"};
        return monthNames[month - 1];
    }

    // ---------------- PDF Export ----------------
    public ResponseEntity<byte[]> exportPdf(Long userId, Integer month, Integer year) {
        try {
            ReportDto report = month != null
                    ? getMonthlyReport(userId, month, year)
                    : getAnnualReport(userId, year);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            String reportTitle = month != null ? 
                "Monthly Expense Report - " + getMonthName(month) + " " + year :
                "Annual Expense Report - " + year;
            Paragraph title = new Paragraph(reportTitle, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            // Category breakdown table
            Paragraph categoryHeader = new Paragraph("Category Breakdown", headerFont);
            document.add(categoryHeader);
            PdfPTable categoryTable = new PdfPTable(2);
            categoryTable.setWidthPercentage(100);
            categoryTable.addCell(new Phrase("Category", headerFont));
            categoryTable.addCell(new Phrase("Amount", headerFont));
            for (Map.Entry<String, Double> entry : report.getCategoryExpenses().entrySet()) {
                categoryTable.addCell(new Phrase(entry.getKey(), bodyFont));
                categoryTable.addCell(new Phrase("₹" + entry.getValue(), bodyFont));
            }
            document.add(categoryTable);
            document.add(new Paragraph(" "));

            // Monthly breakdown for annual reports
            if (month == null && report.getMonthlyExpenses() != null) {
                Paragraph monthlyHeader = new Paragraph("Monthly Breakdown", headerFont);
                document.add(monthlyHeader);
                PdfPTable monthlyTable = new PdfPTable(2);
                monthlyTable.setWidthPercentage(100);
                monthlyTable.addCell(new Phrase("Month", headerFont));
                monthlyTable.addCell(new Phrase("Amount", headerFont));
                
                // ✅ Sort months in chronological order (Jan to Dec)
                String[] monthOrder = {"January", "February", "March", "April", "May", "June",
                                    "July", "August", "September", "October", "November", "December"};
                
                for (String monthName : monthOrder) {
                    Double amount = report.getMonthlyExpenses().get(monthName);
                    monthlyTable.addCell(new Phrase(monthName, bodyFont));
                    monthlyTable.addCell(new Phrase("₹" + (amount != null ? amount : 0.0), bodyFont));
                }
                document.add(monthlyTable);
                document.add(new Paragraph(" "));
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total Spent: ₹" + report.getTotalSpent(), bodyFont));
            
            // ✅ Add "(current month)" label for annual reports
            if (month == null) {
                document.add(new Paragraph("Budget (current month): ₹" + report.getBudget(), bodyFont));
                document.add(new Paragraph("Remaining (current month): ₹" + report.getRemaining(), bodyFont));
            } else {
                document.add(new Paragraph("Budget: ₹" + report.getBudget(), bodyFont));
                document.add(new Paragraph("Remaining: ₹" + report.getRemaining(), bodyFont));
            }
            document.add(new Paragraph(" "));

            // ✅ Pie chart - Category breakdown
            DefaultPieDataset pieDataset = new DefaultPieDataset();
            report.getCategoryExpenses().forEach(pieDataset::setValue);
            String pieChartTitle = month != null ? "Category Wise Expenses" : "Annual Category Breakdown";
            JFreeChart pieChart = ChartFactory.createPieChart(pieChartTitle, pieDataset, true, true, false);
            java.awt.image.BufferedImage chartImage = pieChart.createBufferedImage(500, 300);
            Image chart = Image.getInstance(chartImage, null);
            chart.setAlignment(Element.ALIGN_CENTER);
            document.add(chart);

            // ✅ Bar chart - Different charts for monthly vs annual
            if (month != null) {
                // Monthly: Category breakdown bar chart
                DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
                report.getCategoryExpenses().forEach((k, v) -> barDataset.addValue(v, "Amount", k));
                JFreeChart barChart = ChartFactory.createBarChart("Expenses by Category", "Category", "Amount", barDataset);
                java.awt.image.BufferedImage barChartImage = barChart.createBufferedImage(500, 300);
                Image barChartPdf = Image.getInstance(barChartImage, null);
                barChartPdf.setAlignment(Element.ALIGN_CENTER);
                document.add(barChartPdf);
            } else {
                // Annual: Monthly breakdown bar chart
                if (report.getMonthlyExpenses() != null) {
                    DefaultCategoryDataset monthlyBarDataset = new DefaultCategoryDataset();
                    report.getMonthlyExpenses().forEach((k, v) -> monthlyBarDataset.addValue(v, "Amount", k));
                    JFreeChart monthlyBarChart = ChartFactory.createBarChart("Monthly Spending Overview", "Month", "Amount", monthlyBarDataset);
                    java.awt.image.BufferedImage monthlyBarChartImage = monthlyBarChart.createBufferedImage(500, 300);
                    Image monthlyBarChartPdf = Image.getInstance(monthlyBarChartImage, null);
                    monthlyBarChartPdf.setAlignment(Element.ALIGN_CENTER);
                    document.add(monthlyBarChartPdf);
                }
            }

            document.close();

            String fileName = "report_" + (month != null ? month : "annual") + "_" + year + ".pdf";

            // ✅ Notification: PDF report exported
            notificationService.createNotification(userId, "Your budget report PDF has been exported successfully.");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(baos.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    // ---------------- Excel Export ----------------
    public ResponseEntity<byte[]> exportExcel(Long userId, Integer month, Integer year) {
        try (Workbook workbook = new XSSFWorkbook()) {
            ReportDto report = month != null
                    ? getMonthlyReport(userId, month, year)
                    : getAnnualReport(userId, year);

            String sheetName = month != null ? 
                getMonthName(month) + " " + year : 
                "Annual Report " + year;
            Sheet sheet = workbook.createSheet(sheetName);
            int rowNum = 0;

            // Category breakdown
            Row categoryHeader = sheet.createRow(rowNum++);
            categoryHeader.createCell(0).setCellValue("Category");
            categoryHeader.createCell(1).setCellValue("Amount");

            for (Map.Entry<String, Double> entry : report.getCategoryExpenses().entrySet()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue());
            }

            // Monthly breakdown for annual reports
            if (month == null && report.getMonthlyExpenses() != null) {
                rowNum++; // Add empty row
                Row monthlyHeader = sheet.createRow(rowNum++);
                monthlyHeader.createCell(0).setCellValue("Month");
                monthlyHeader.createCell(1).setCellValue("Amount");

                // ✅ Sort months in chronological order (Jan to Dec)
                String[] monthOrder = {"January", "February", "March", "April", "May", "June",
                                    "July", "August", "September", "October", "November", "December"};
                
                for (String monthName : monthOrder) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(monthName);
                    Double amount = report.getMonthlyExpenses().get(monthName);
                    row.createCell(1).setCellValue(amount != null ? amount : 0.0);
                }
            }

            rowNum++; // Add empty row
            Row totalRow = sheet.createRow(rowNum++);
            totalRow.createCell(0).setCellValue("Total Spent");
            totalRow.createCell(1).setCellValue(report.getTotalSpent());

            // ✅ Add "(current month)" label for annual reports
            Row budgetRow = sheet.createRow(rowNum++);
            if (month == null) {
                budgetRow.createCell(0).setCellValue("Budget (current month)");
            } else {
                budgetRow.createCell(0).setCellValue("Budget");
            }
            budgetRow.createCell(1).setCellValue(report.getBudget());

            Row remainingRow = sheet.createRow(rowNum++);
            if (month == null) {
                remainingRow.createCell(0).setCellValue("Remaining (current month)");
            } else {
                remainingRow.createCell(0).setCellValue("Remaining");
            }
            remainingRow.createCell(1).setCellValue(report.getRemaining());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            String fileName = "report_" + (month != null ? month : "annual") + "_" + year + ".xlsx";

            // ✅ Notification: Excel report exported
            notificationService.createNotification(userId, "Your budget report Excel file has been exported successfully.");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(baos.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Error generating Excel", e);
        }
    }
}
