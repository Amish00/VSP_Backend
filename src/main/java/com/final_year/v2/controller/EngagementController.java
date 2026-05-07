package com.final_year.v2.controller;

import com.final_year.v2.service.EngagementService;
import com.final_year.v2.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/engagement")
@CrossOrigin(origins = "http://localhost:5173")
public class EngagementController {

    @Autowired
    private EngagementService engagementService;

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/watch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> recordWatchTime(@RequestBody Map<String, Object> payload) {
        Long userId = paymentService.getCurrentUserId();
        Long videoId = ((Number) payload.get("videoId")).longValue();
        int watchTimeSeconds = ((Number) payload.get("watchTimeSeconds")).intValue();

        engagementService.recordWatchTime(userId, videoId, watchTimeSeconds);
        return ResponseEntity.ok(Map.of("message", "Watch time recorded"));
    }

    @PostMapping("/view")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> recordView(@RequestBody Map<String, Long> payload) {
        Long userId = paymentService.getCurrentUserId();
        Long videoId = payload.get("videoId");

        engagementService.recordView(userId, videoId);
        return ResponseEntity.ok(Map.of("message", "View recorded"));
    }
}