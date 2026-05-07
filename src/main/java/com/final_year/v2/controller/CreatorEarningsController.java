package com.final_year.v2.controller;

import com.final_year.v2.model.MonthlyEarnings;
import com.final_year.v2.model.PayoutRequest;
import com.final_year.v2.service.EarningsService;
import com.final_year.v2.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/creator/earnings")
@PreAuthorize("hasRole('CREATOR')")
public class CreatorEarningsController {

    @Autowired private EarningsService earningsService;
    @Autowired private PaymentService paymentService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Long userId = paymentService.getCurrentUserId();
        BigDecimal total = earningsService.getTotalEarned(userId);
        BigDecimal paid = earningsService.getTotalPaidOut(userId);
        BigDecimal pending = total.subtract(paid);
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalEarned", total);
        summary.put("paidOut", paid);
        summary.put("pending", pending);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/history")
    public ResponseEntity<List<MonthlyEarnings>> getHistory() {
        Long userId = paymentService.getCurrentUserId();
        return ResponseEntity.ok(earningsService.getCreatorEarningsHistory(userId));
    }

    @GetMapping("/payouts")
    public ResponseEntity<List<PayoutRequest>> getPayoutHistory() {
        Long userId = paymentService.getCurrentUserId();
        return ResponseEntity.ok(earningsService.getCreatorPayoutHistory(userId));
    }

    @PostMapping("/request")
    public ResponseEntity<PayoutRequest> requestPayout(@RequestBody Map<String, Object> payload) {
        Long userId = paymentService.getCurrentUserId();
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());
        String method = (String) payload.get("method");
        String accountDetails = (String) payload.get("accountDetails");
        return ResponseEntity.ok(earningsService.requestPayout(userId, amount, method, accountDetails));
    }
}