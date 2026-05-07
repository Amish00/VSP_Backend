package com.final_year.v2.scheduler;

import com.final_year.v2.model.MonthlyEarnings;
import com.final_year.v2.model.RevenueSubscription;
import com.final_year.v2.model.User;
import com.final_year.v2.repository.MonthlyEarningsRepository;
import com.final_year.v2.repository.RevenueSubscriptionRepository;
import com.final_year.v2.repository.UserRepository;
import com.final_year.v2.service.EmailService;
import com.final_year.v2.service.MonthlyEarningsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MonthlyEarningsScheduler {

    @Autowired
    private MonthlyEarningsService monthlyEarningsService;

    @Autowired
    private MonthlyEarningsRepository monthlyEarningsRepository;

    @Autowired
    private RevenueSubscriptionRepository revenueSubscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    // Run at 00:05 on the 1st of every month
    @Scheduled(cron = "0 5 0 1 * ?")
    public void calculatePreviousMonthEarnings() {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        monthlyEarningsService.calculateMonthlyEarnings(previousMonth);

        // After calculation, send reports
        sendCreatorEarningsReports(previousMonth);
        sendAdminRevenueReport(previousMonth);
    }

    private void sendCreatorEarningsReports(YearMonth month) {
        String monthStr = month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        List<MonthlyEarnings> allEarnings = monthlyEarningsRepository.findAll(); // or fetch by month
        // Group by creatorId
        var creatorEarningsMap = allEarnings.stream()
                .filter(e -> e.getMonthYear().equals(monthStr))
                .collect(Collectors.groupingBy(MonthlyEarnings::getCreatorId));

        for (Long creatorId : creatorEarningsMap.keySet()) {
            userRepository.findById(creatorId).ifPresent(creator -> {
                // Get all past earnings for this creator for total calculation
                List<MonthlyEarnings> allCreatorEarnings = monthlyEarningsRepository.findAllByCreatorIdOrderByMonthYearDesc(creatorId);
                BigDecimal totalEarned = allCreatorEarnings.stream()
                        .map(MonthlyEarnings::getEarningsAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal pending = totalEarned.subtract(
                        // Assume you have a method to get paid out amount; if not, we can skip or set 0 for now
                        BigDecimal.ZERO
                );
                // Send email report (only if creator has any earnings)
                if (!allCreatorEarnings.isEmpty()) {
                    emailService.sendMonthlyEarningsReport(creator, allCreatorEarnings, totalEarned, pending);
                }
            });
        }
    }

    private void sendAdminRevenueReport(YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

        List<RevenueSubscription> activeSubs = revenueSubscriptionRepository.findActiveBetween(start, end);
        BigDecimal totalRevenue = activeSubs.stream()
                .map(RevenueSubscription::getMonthlyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal platformFee = totalRevenue.multiply(BigDecimal.valueOf(30))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal creatorPool = totalRevenue.subtract(platformFee);

        int activeSubscriptionsCount = activeSubs.size();
        long activeCreatorsCount = activeSubs.stream()
                .map(RevenueSubscription::getUserId)
                .distinct()
                .count();

        emailService.sendMonthlyRevenueReport(month, totalRevenue, platformFee, creatorPool, activeSubscriptionsCount, (int) activeCreatorsCount);
    }
}