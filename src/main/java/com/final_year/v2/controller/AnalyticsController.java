package com.final_year.v2.controller;

import com.final_year.v2.security.UserDetailsImpl;
import com.final_year.v2.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/creator/analytics")
@PreAuthorize("hasRole('CREATOR')")
@RequiredArgsConstructor
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    private final AnalyticsService analyticsService;


    @GetMapping("/views-over-time")
    public ResponseEntity<List<Map<String, Object>>> getViewsOverTime(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(defaultValue = "180") int days) {

        Long userId = currentUser.getId();
        log.info("Fetching views-over-time for user ID: {}, days: {}", userId, days);

        try {
            List<Map<String, Object>> data = analyticsService.getViewsOverTime(userId, days);
            log.info("Returning {} data points", data.size());
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Error in getViewsOverTime for user {}: {}", userId, e.getMessage(), e);
            throw e; // let global exception handler deal with it
        }
    }

    @GetMapping("/content-breakdown")
    public ResponseEntity<List<Map<String, Object>>> getContentBreakdown(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        Long userId = currentUser.getId();
        log.info("Fetching content breakdown for user ID: {}", userId);
        return ResponseEntity.ok(analyticsService.getContentBreakdown(userId));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(defaultValue = "30") int days) {

        Long userId = currentUser.getId();
        log.info("Fetching summary stats for user ID: {}, days: {}", userId, days);
        return ResponseEntity.ok(analyticsService.getSummaryStats(userId, days));
    }

    @GetMapping("/top-videos")
    public ResponseEntity<List<Map<String, Object>>> getTopVideos(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(defaultValue = "5") int limit) {

        Long userId = currentUser.getId();
        log.info("Fetching top {} videos for user ID: {}", limit, userId);
        return ResponseEntity.ok(analyticsService.getTopVideosByViews(userId, limit));
    }
}