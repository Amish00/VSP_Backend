package com.final_year.v2.controller;

import com.final_year.v2.service.AnalyticsService;
import com.final_year.v2.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/creator/analytics")
@PreAuthorize("hasRole('CREATOR')")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final PaymentService paymentService;

    @GetMapping("/views-over-time")
    public ResponseEntity<List<Map<String, Object>>> getViewsOverTime(
            @RequestParam(defaultValue = "180") int days) {
        Long userId = paymentService.getCurrentUserId();
        return ResponseEntity.ok(analyticsService.getViewsOverTime(userId, days));
    }

    @GetMapping("/content-breakdown")
    public ResponseEntity<List<Map<String, Object>>> getContentBreakdown() {
        Long userId = paymentService.getCurrentUserId();
        return ResponseEntity.ok(analyticsService.getContentBreakdown(userId));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(@RequestParam(defaultValue = "30") int days) {
        Long userId = paymentService.getCurrentUserId();
        return ResponseEntity.ok(analyticsService.getSummaryStats(userId, days));
    }

    @GetMapping("/top-videos")
    public ResponseEntity<List<Map<String, Object>>> getTopVideos(@RequestParam(defaultValue = "5") int limit) {
        Long userId = paymentService.getCurrentUserId();
        return ResponseEntity.ok(analyticsService.getTopVideosByViews(userId, limit));
    }
}