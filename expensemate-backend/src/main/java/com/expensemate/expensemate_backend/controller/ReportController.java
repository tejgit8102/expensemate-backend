package com.expensemate.expensemate_backend.controller;

import com.expensemate.expensemate_backend.dto.ReportDto;
import com.expensemate.expensemate_backend.security.JwtUtil;
import com.expensemate.expensemate_backend.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final JwtUtil jwtUtil;

    public ReportController(ReportService reportService, JwtUtil jwtUtil) {
        this.reportService = reportService;
        this.jwtUtil = jwtUtil;
    }

    // ----------------- Monthly Report -----------------
    @GetMapping("/monthly")
    public ResponseEntity<ReportDto> getMonthlyReport(
            HttpServletRequest request,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        Long userId = jwtUtil.extractUserIdFromRequest(request);

        // Default month/year to current if not provided
        LocalDate now = LocalDate.now();
        if (month == null) month = now.getMonthValue();
        if (year == null) year = now.getYear();

        // Validate month
        if (month < 1 || month > 12) {
            return ResponseEntity.badRequest().body(null);
        }

        return ResponseEntity.ok(reportService.getMonthlyReport(userId, month, year));
    }

    // ----------------- Annual Report -----------------
    @GetMapping("/annual")
    public ResponseEntity<ReportDto> getAnnualReport(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year) {

        Long userId = jwtUtil.extractUserIdFromRequest(request);
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        return ResponseEntity.ok(reportService.getAnnualReport(userId, year));
    }

    // ----------------- Export PDF -----------------
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            HttpServletRequest request,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        Long userId = jwtUtil.extractUserIdFromRequest(request);

        // Default year to current if not provided, but keep month null for annual reports
        LocalDate now = LocalDate.now();
        if (year == null) year = now.getYear();
        // Don't set month to current month if it's null - this indicates annual report

        // Dynamic file name
        String fileName = "report_" + (month != null ? month : "annual") + "_" + year + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .body(reportService.exportPdf(userId, month, year).getBody());
    }

    // ----------------- Export Excel -----------------
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            HttpServletRequest request,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        Long userId = jwtUtil.extractUserIdFromRequest(request);

        // Default year to current if not provided, but keep month null for annual reports
        LocalDate now = LocalDate.now();
        if (year == null) year = now.getYear();
        // Don't set month to current month if it's null - this indicates annual report

        // Dynamic file name
        String fileName = "report_" + (month != null ? month : "annual") + "_" + year + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .body(reportService.exportExcel(userId, month, year).getBody());
    }
}
