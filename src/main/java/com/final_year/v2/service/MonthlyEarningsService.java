package com.final_year.v2.service;

import com.final_year.v2.model.*;
import com.final_year.v2.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MonthlyEarningsService {

    @Autowired
    private RevenueSubscriptionRepository revenueSubscriptionRepository;
    @Autowired
    private WatchHistoryRepository watchHistoryRepository;
    @Autowired
    private ViewRecordRepository viewRecordRepository;
    @Autowired
    private MonthlyEarningsRepository monthlyEarningsRepository;

    @Value("${platform.fee.percentage:30}")
    private int platformFeePercent;

    @Transactional
    public void calculateMonthlyEarnings(YearMonth yearMonth) {
        String monthYear = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        if (monthlyEarningsRepository.existsByMonthYear(monthYear)) {
            return; // already calculated
        }

        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // 1. Total monthly revenue from active subscriptions
        BigDecimal totalRevenue = revenueSubscriptionRepository.findActiveBetween(start, end)
                .stream()
                .map(RevenueSubscription::getMonthlyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRevenue.compareTo(BigDecimal.ZERO) <= 0) return;

        // 2. Platform fee and creator pool
        BigDecimal platformFee = totalRevenue.multiply(BigDecimal.valueOf(platformFeePercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal creatorPool = totalRevenue.subtract(platformFee);

        // 3. Weighted watch time and views per creator
        Map<Long, BigDecimal> watchMap = new HashMap<>();
        List<Object[]> watchResults = watchHistoryRepository.getWeightedWatchTimeByCreator(start, end);
        for (Object[] row : watchResults) {
            watchMap.put((Long) row[0], (BigDecimal) row[1]);
        }

        Map<Long, BigDecimal> viewMap = new HashMap<>();
        List<Object[]> viewResults = viewRecordRepository.getWeightedViewsByCreator(start, end);
        for (Object[] row : viewResults) {
            viewMap.put((Long) row[0], (BigDecimal) row[1]);
        }

        // 4. Totals
        BigDecimal totalWatch = watchMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalViews = viewMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Calculate earnings per creator
        for (Long creatorId : watchMap.keySet()) {
            BigDecimal watchTime = watchMap.getOrDefault(creatorId, BigDecimal.ZERO);
            BigDecimal views = viewMap.getOrDefault(creatorId, BigDecimal.ZERO);

            BigDecimal watchPct = totalWatch.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : watchTime.divide(totalWatch, 10, RoundingMode.HALF_UP);
            BigDecimal viewPct = totalViews.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : views.divide(totalViews, 10, RoundingMode.HALF_UP);

            BigDecimal score = watchPct.add(viewPct).divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
            BigDecimal earnings = creatorPool.multiply(score).setScale(2, RoundingMode.HALF_UP);

            MonthlyEarnings me = new MonthlyEarnings();
            me.setCreatorId(creatorId);
            me.setMonthYear(monthYear);
            me.setEarningsAmount(earnings);
            monthlyEarningsRepository.save(me);
        }
    }
}