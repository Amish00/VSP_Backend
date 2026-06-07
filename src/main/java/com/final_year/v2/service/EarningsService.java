package com.final_year.v2.service;

import com.final_year.v2.dto.AdminPayoutDTO;
import com.final_year.v2.model.*;
import com.final_year.v2.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EarningsService {

    @Autowired
    private MonthlyEarningsRepository monthlyEarningsRepository;

    @Autowired
    private PayoutRequestRepository payoutRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RevenueSubscriptionRepository revenueSubscriptionRepository;

    @Autowired
    private EmailService emailService;

    // ----- CREATOR EARNINGS -----
    public BigDecimal getTotalEarned(Long creatorId) {
        return monthlyEarningsRepository.sumEarningsByCreatorId(creatorId).orElse(BigDecimal.ZERO);
    }

    public BigDecimal getTotalPaidOut(Long creatorId) {
        return payoutRequestRepository.sumAmountByCreatorIdAndStatus(creatorId, "PROCESSED").orElse(BigDecimal.ZERO);
    }

    public BigDecimal getPendingPayout(Long creatorId) {
        return getTotalEarned(creatorId).subtract(getTotalPaidOut(creatorId));
    }

    public List<MonthlyEarnings> getCreatorEarningsHistory(Long creatorId) {
        return monthlyEarningsRepository.findAllByCreatorIdOrderByMonthYearDesc(creatorId);
    }

    public List<PayoutRequest> getCreatorPayoutHistory(Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator not found"));
        return payoutRequestRepository.findByCreatorOrderByRequestedAtDesc(creator);
    }

    @Transactional
    public PayoutRequest requestPayout(Long creatorId, BigDecimal amount, String method, String accountDetails) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator not found"));
        BigDecimal available = getPendingPayout(creatorId);
        if (amount.compareTo(available) > 0 || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid payout amount");
        }
        PayoutRequest req = new PayoutRequest();
        req.setCreator(creator);
        req.setAmount(amount);
        req.setWithdrawalMethod(method);
        req.setAccountDetails(accountDetails);
        req.setStatus("PENDING");
        PayoutRequest saved = payoutRequestRepository.save(req);

        // Send email notification to admin
        emailService.sendPayoutRequestNotification(saved, creator);
        return saved;
    }

    // ----- ADMIN PAYOUT ACTIONS -----
    // Returns DTOs to avoid circular JSON serialization
    public List<AdminPayoutDTO> getPendingPayoutsAsDTO() {
        List<PayoutRequest> pending = payoutRequestRepository.findByStatusOrderByRequestedAtDesc("PENDING");
        return pending.stream()
                .map(this::convertToAdminDTO)
                .collect(Collectors.toList());
    }

    // Original method kept for internal use if needed
    public List<PayoutRequest> getPendingPayouts() {
        return payoutRequestRepository.findByStatusOrderByRequestedAtDesc("PENDING");
    }

    @Transactional
    public void processPayout(Long payoutId) {
        PayoutRequest req = payoutRequestRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Payout not found"));
        req.setStatus("PROCESSED");
        req.setProcessedAt(LocalDateTime.now());
        payoutRequestRepository.save(req);
    }

    @Transactional
    public void rejectPayout(Long payoutId, String reason) {
        PayoutRequest req = payoutRequestRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Payout not found"));
        req.setStatus("REJECTED");
        req.setRejectionReason(reason);
        req.setProcessedAt(LocalDateTime.now());
        payoutRequestRepository.save(req);
    }

    // ----- PLATFORM REVENUE (ADMIN) -----
    public BigDecimal getTotalRevenue(LocalDateTime start, LocalDateTime end) {
        return revenueSubscriptionRepository.findActiveBetween(start, end)
                .stream().map(RevenueSubscription::getMonthlyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getPlatformFee(LocalDateTime start, LocalDateTime end) {
        return getTotalRevenue(start, end).multiply(BigDecimal.valueOf(30))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getCreatorPool(LocalDateTime start, LocalDateTime end) {
        return getTotalRevenue(start, end).subtract(getPlatformFee(start, end));
    }

    public List<Map<String, Object>> getMonthlyRevenueBreakdown(int monthsBack) {
        List<Map<String, Object>> result = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = monthsBack; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);
            BigDecimal total = getTotalRevenue(start, end);
            BigDecimal platformFee = getPlatformFee(start, end);
            BigDecimal creatorPool = getCreatorPool(start, end);
            Map<String, Object> map = new HashMap<>();
            map.put("month", ym.format(DateTimeFormatter.ofPattern("MMM")));
            map.put("total", total);
            map.put("platformFee", platformFee);
            map.put("creatorPool", creatorPool);
            result.add(map);
        }
        return result;
    }

    // ----- Helper: convert PayoutRequest -> AdminPayoutDTO -----
    private AdminPayoutDTO convertToAdminDTO(PayoutRequest request) {
        AdminPayoutDTO.CreatorSummary creatorSummary = null;
        if (request.getCreator() != null) {
            creatorSummary = new AdminPayoutDTO.CreatorSummary(
                    request.getCreator().getId(),
                    request.getCreator().getUsername(),
                    request.getCreator().getEmail()
            );
        }
        return new AdminPayoutDTO(
                request.getId(),
                request.getAmount(),
                request.getWithdrawalMethod(),
                request.getAccountDetails(),
                request.getStatus(),
                request.getRequestedAt(),
                request.getProcessedAt(),
                request.getRejectionReason(),
                creatorSummary
        );
    }
}