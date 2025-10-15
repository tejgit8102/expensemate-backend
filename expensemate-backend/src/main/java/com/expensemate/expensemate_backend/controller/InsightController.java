package com.expensemate.expensemate_backend.controller;

import com.expensemate.expensemate_backend.dto.InsightDto;
import com.expensemate.expensemate_backend.security.JwtUtil;
import com.expensemate.expensemate_backend.service.InsightService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/insights")
public class InsightController {

    private final InsightService insightService;
    private final JwtUtil jwtUtil;

    public InsightController(InsightService insightService, JwtUtil jwtUtil) {
        this.insightService = insightService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Get AI-powered user spending insights.
     *
     * @param request HttpServletRequest for JWT extraction
     * @param month   "all", "current", or "YYYY-MM" to filter expenses
     * @return InsightDto containing spending metrics, trends, unusual expenses, and budget health
     */
    @GetMapping
    public ResponseEntity<InsightDto> getUserInsights(
            HttpServletRequest request,
            @RequestParam(defaultValue = "all") String month) {

        try {
            // Extract user ID from JWT
            Long userId = jwtUtil.extractUserIdFromRequest(request);
            System.out.println("InsightController: Extracted userId: " + userId);
            System.out.println("InsightController: Request month: " + month);
            
            if (userId == null) {
                System.out.println("InsightController: No userId found in request");
                return ResponseEntity.status(401).body(null);
            }

            // Generate insights for the user and requested month
            InsightDto insights = insightService.generateUserInsights(userId, month);
            System.out.println("InsightController: Generated insights: " + insights);

            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            System.out.println("InsightController: Error generating insights: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Export AI-powered insights as PDF.
     *
     * @param request HttpServletRequest for JWT extraction
     * @param month   "all", "current", or "YYYY-MM" to filter expenses
     * @return PDF file containing the insights analysis
     */
    @GetMapping("/export-pdf")
    public ResponseEntity<byte[]> exportInsightsPDF(
            HttpServletRequest request,
            @RequestParam(defaultValue = "all") String month) {

        try {
            // Extract user ID from JWT
            Long userId = jwtUtil.extractUserIdFromRequest(request);
            System.out.println("InsightController: PDF Export - Extracted userId: " + userId);
            System.out.println("InsightController: PDF Export - Request month: " + month);

            if (userId == null) {
                System.out.println("InsightController: PDF Export - No userId found in request");
                return ResponseEntity.status(401).body(null);
            }

            // Generate PDF for the user and requested month
            byte[] pdfBytes = insightService.exportInsightsPDF(userId, month);
            System.out.println("InsightController: PDF Export - Generated PDF, size: " + pdfBytes.length + " bytes");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "AI_Insights_" + month + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            System.out.println("InsightController: PDF Export - Error generating PDF: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
}
