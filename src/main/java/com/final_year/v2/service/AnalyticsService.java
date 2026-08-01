package com.final_year.v2.service;

import com.final_year.v2.constaint.VideoStatus;
import com.final_year.v2.model.Video;
import com.final_year.v2.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final VideoRepository videoRepository;
    private final ViewRecordRepository viewRecordRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final SubscriptionRepository subscriptionRepository;

    public List<Map<String, Object>> getViewsOverTime(Long creatorId, int days) {
        if (creatorId == null) {
            log.warn("getViewsOverTime called with null creatorId");
            return Collections.emptyList();
        }

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> dailyViews = viewRecordRepository.getDailyViewsForCreator(creatorId, startDate);

        // Map date -> view count
        Map<LocalDate, Long> viewsMap = new HashMap<>();
        for (Object[] row : dailyViews) {
            // row[0] = date (java.sql.Date or LocalDate), row[1] = total views (Long)
            LocalDate date;
            if (row[0] instanceof java.sql.Date) {
                date = ((java.sql.Date) row[0]).toLocalDate();
            } else if (row[0] instanceof LocalDate) {
                date = (LocalDate) row[0];
            } else {
                log.warn("Unexpected date type: {}", row[0].getClass());
                continue;
            }
            Long count = ((Number) row[1]).longValue();
            viewsMap.put(date, count);
        }

        // Build result list with all dates in range (today - days .. today)
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

        for (int i = days; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Long views = viewsMap.getOrDefault(date, 0L);

            Map<String, Object> point = new HashMap<>();
            point.put("date", date.format(formatter));
            point.put("views", views);
            result.add(point);
        }

        log.debug("getViewsOverTime for creator {} returned {} entries", creatorId, result.size());
        return result;
    }

    public List<Map<String, Object>> getContentBreakdown(Long creatorId) {
        if (creatorId == null) return Collections.emptyList();

        List<Object[]> categoryCounts = videoRepository.countByCategoryForCreator(creatorId);
        long total = categoryCounts.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        List<Map<String, Object>> breakdown = new ArrayList<>();
        for (Object[] row : categoryCounts) {
            String category = (String) row[0];
            long count = ((Number) row[1]).longValue();
            double percent = total == 0 ? 0 : (count * 100.0 / total);
            Map<String, Object> item = new HashMap<>();
            item.put("name", category == null ? "Uncategorized" : category);
            item.put("value", Math.round(percent));
            breakdown.add(item);
        }
        return breakdown;
    }

    public Map<String, Object> getSummaryStats(Long creatorId, int days) {
        if (creatorId == null) return Map.of();

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        Long totalViews = viewRecordRepository.countByVideoUserIdAndCreatedAtAfter(creatorId, startDate);
        totalViews = totalViews == null ? 0L : totalViews;

        BigDecimal watchTimeSeconds = watchHistoryRepository.sumWeightedWatchTimeByCreator(creatorId, startDate);
        double watchTimeHours = watchTimeSeconds == null ? 0 : watchTimeSeconds.doubleValue() / 3600.0;

        long newSubscribers = subscriptionRepository.countNewSubscribersSince(creatorId, startDate);

        Long totalEngagement = videoRepository.sumLikesAndCommentsByCreatorSince(creatorId, startDate);
        totalEngagement = totalEngagement == null ? 0L : totalEngagement;
        double ctr = totalViews == 0 ? 0.0 : (totalEngagement.doubleValue() / totalViews) * 100;

        Map<String, Object> stats = new HashMap<>();
        stats.put("views", totalViews);
        stats.put("watchTimeHours", watchTimeHours);
        stats.put("newSubscribers", newSubscribers);
        stats.put("ctr", Math.round(ctr * 10) / 10.0);
        return stats;
    }


    public List<Map<String, Object>> getTopVideosByViews(Long creatorId, int limit) {
        if (creatorId == null || limit <= 0) return Collections.emptyList();

        Pageable pageable = PageRequest.of(0, limit);
        Page<Video> videoPage = videoRepository.findByUserIdAndStatusOrderByViewCountDesc(
                creatorId, VideoStatus.APPROVED, pageable);
        List<Video> videos = videoPage.getContent();

        if (videos.isEmpty()) return Collections.emptyList();

        long maxViews = videos.get(0).getViewCount();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Video video : videos) {
            Map<String, Object> item = new HashMap<>();
            item.put("title", video.getTitle());
            item.put("views", video.getViewCount());
            double percentage = maxViews == 0 ? 0 : (video.getViewCount() * 100.0 / maxViews);
            item.put("percentage", Math.round(percentage));
            result.add(item);
        }
        return result;
    }
}