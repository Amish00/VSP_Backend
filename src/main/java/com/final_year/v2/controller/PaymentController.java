package com.final_year.v2.controller;

import com.final_year.v2.dto.PaymentInitiateRequest;
import com.final_year.v2.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/initiate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> initiate(@RequestBody PaymentInitiateRequest request) {
        Map<String, Object> response = paymentService.initiatePayment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/callback/esewa")
    public void esewaSuccess(@RequestParam String data, HttpServletResponse response) throws IOException {
        paymentService.verifyEsewa(data, true);
        response.sendRedirect("http://localhost:5173/payment/success");
    }

    @GetMapping("/callback/esewa/failure")
    public void esewaFailure(HttpServletResponse response) throws IOException {
        response.sendRedirect("http://localhost:5173/payment/failure");
    }

    @GetMapping("/callback/khalti")
    public void khaltiCallback(@RequestParam String pidx, HttpServletResponse response) throws IOException {
        paymentService.verifyKhalti(pidx);
        response.sendRedirect("http://localhost:5173/payment/success");
    }

    @PostMapping("/upgrade-free")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> upgradeToFree() {
        Long userId = paymentService.getCurrentUserId();
        paymentService.upgradeToFreePlan(userId);
        return ResponseEntity.ok(Map.of("message", "Plan updated to Free"));
    }
}
