package com.final_year.v2.controller;

import com.final_year.v2.service.EngagementService;
import com.final_year.v2.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<?> recordView(@RequestBody Map<String, Long> payload,
                                        @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
                                        HttpServletRequest request) {
        Long videoId = payload.get("videoId");
        Long userId = null;

        // Try to get authenticated user
        try {
            userId = paymentService.getCurrentUserId();
        } catch (Exception e) {
            // not authenticated – leave userId as null
        }

        // If not authenticated, sessionId is mandatory
        if (userId == null && (sessionId == null || sessionId.trim().isEmpty())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Session ID required for anonymous views"));
        }

        boolean counted = engagementService.recordView(userId, sessionId, videoId);
        return ResponseEntity.ok(Map.of("counted", counted));
    }
}