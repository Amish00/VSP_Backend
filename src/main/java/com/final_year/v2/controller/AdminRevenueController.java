package com.final_year.v2.controller;

import com.final_year.v2.constaint.Role;
import com.final_year.v2.dto.PaymentProjection;
import com.final_year.v2.dto.PaymentResponse;
import com.final_year.v2.model.MonthlyEarnings;
import com.final_year.v2.model.PayoutRequest;
import com.final_year.v2.model.RevenueRecord;
import com.final_year.v2.model.User;
import com.final_year.v2.repository.MonthlyEarningsRepository;
import com.final_year.v2.repository.PaymentRepository;
import com.final_year.v2.repository.RevenueRecordRepository;
import com.final_year.v2.repository.UserRepository;
import com.final_year.v2.service.EarningsService;
import com.final_year.v2.service.MonthlyEarningsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/revenue")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRevenueController {

    @Autowired
    private EarningsService earningsService;

    @Autowired
    private MonthlyEarningsService monthlyEarningsService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MonthlyEarningsRepository monthlyEarningsRepository;

    @GetMapping("/total")
    public ResponseEntity<Map<String, Object>> getTotalRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        BigDecimal total = earningsService.getTotalRevenue(start, end);
        BigDecimal fee = earningsService.getPlatformFee(start, end);
        BigDecimal pool = earningsService.getCreatorPool(start, end);
        return ResponseEntity.ok(Map.of("totalRevenue", total, "platformFee", fee, "creatorPool", pool));
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyRevenue(@RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(earningsService.getMonthlyRevenueBreakdown(months));
    }

    @GetMapping("/payouts/pending")
    public ResponseEntity<List<PayoutRequest>> getPendingPayouts() {
        return ResponseEntity.ok(earningsService.getPendingPayouts());
    }

    @PostMapping("/payouts/{id}/process")
    public ResponseEntity<?> processPayout(@PathVariable Long id) {
        earningsService.processPayout(id);
        return ResponseEntity.ok(Map.of("message", "Payout processed"));
    }

    @PostMapping("/payouts/{id}/reject")
    public ResponseEntity<?> rejectPayout(@PathVariable Long id, @RequestBody Map<String, String> body) {
        earningsService.rejectPayout(id, body.get("reason"));
        return ResponseEntity.ok(Map.of("message", "Payout rejected"));
    }

    @PostMapping("/calculate-earnings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> calculateEarnings(@RequestParam String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        monthlyEarningsService.calculateMonthlyEarnings(ym);
        return ResponseEntity.ok(Map.of("message", "Earnings calculated for " + yearMonth));
    }

    @GetMapping("/records")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PaymentProjection>> getPaymentRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(paymentRepository.findAllWithUsername(pageable));
    }

    @GetMapping("/earnings/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllCreatorEarnings(@RequestParam(defaultValue = "12") int months) {
        List<Map<String, Object>> result = new ArrayList<>();
        // Get all creators (users with role CREATOR)
        List<User> creators = userRepository.findByRole(Role.CREATOR);
        for (User creator : creators) {
            List<MonthlyEarnings> earnings = monthlyEarningsRepository.findAllByCreatorIdOrderByMonthYearDesc(creator.getId());
            for (MonthlyEarnings e : earnings) {
                Map<String, Object> map = new HashMap<>();
                map.put("creatorName", creator.getUsername());
                map.put("monthYear", e.getMonthYear());
                map.put("earningsAmount", e.getEarningsAmount());
                result.add(map);
            }
        }
        // Sort by month-year descending
        result.sort((a,b) -> ((String)b.get("monthYear")).compareTo((String)a.get("monthYear")));
        return ResponseEntity.ok(result);
    }
}