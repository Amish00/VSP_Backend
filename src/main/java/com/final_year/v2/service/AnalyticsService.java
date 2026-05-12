package com.final_year.v2.service;

import com.final_year.v2.constaint.VideoStatus;
import com.final_year.v2.model.Video;
import com.final_year.v2.repository.*;
import lombok.RequiredArgsConstructor;
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

    private final VideoRepository videoRepository;
    private final ViewRecordRepository viewRecordRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final SubscriptionRepository subscriptionRepository;

    // Views over time (daily)
    public List<Map<String, Object>> getViewsOverTime(Long creatorId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> dailyViews = viewRecordRepository.getDailyViewsForCreator(creatorId, startDate);
        Map<LocalDate, Long> viewsMap = new HashMap<>();
        for (Object[] row : dailyViews) {
            // row[0] is date (java.sql.Date or LocalDate), row[1] is count
            LocalDate date = row[0] instanceof java.sql.Date ? ((java.sql.Date) row[0]).toLocalDate() : (LocalDate) row[0];
            Long count = ((Number) row[1]).longValue();
            viewsMap.put(date, count);
        }

        // Fill missing dates with 0
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Long views = viewsMap.getOrDefault(date, 0L);
            Map<String, Object> point = new HashMap<>();
            point.put("date", date.format(DateTimeFormatter.ofPattern("MMM dd")));
            point.put("views", views);
            result.add(point);
        }
        return result;
    }

    // Content breakdown: category percentages
    public List<Map<String, Object>> getContentBreakdown(Long creatorId) {
        List<Object[]> categoryCounts = videoRepository.countByCategoryForCreator(creatorId);
        long total = categoryCounts.stream().mapToLong(row -> ((Number) row[1]).longValue()).sum();
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

    // Summary stats for a period (views, watch time, new subs, CTR)
    public Map<String, Object> getSummaryStats(Long creatorId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        // Total views in period
        Long totalViews = viewRecordRepository.countByVideoUserIdAndCreatedAtAfter(creatorId, startDate);

        // Weighted watch time (seconds) then convert to hours
        BigDecimal watchTimeSeconds = watchHistoryRepository.sumWeightedWatchTimeByCreator(creatorId, startDate);
        double watchTimeHours = watchTimeSeconds == null ? 0 : watchTimeSeconds.doubleValue() / 3600.0;

        // New subscribers
        long newSubscribers = subscriptionRepository.countNewSubscribersSince(creatorId, startDate);

        // CTR = (likes+comments) / views * 100
        Long totalEngagement = videoRepository.sumLikesAndCommentsByCreatorSince(creatorId, startDate);
        double ctr = (totalViews == null || totalViews == 0) ? 0.0 : (totalEngagement.doubleValue() / totalViews) * 100;

        Map<String, Object> stats = new HashMap<>();
        stats.put("views", totalViews == null ? 0 : totalViews);
        stats.put("watchTimeHours", Math.round(watchTimeHours));
        stats.put("newSubscribers", newSubscribers);
        stats.put("ctr", Math.round(ctr * 10) / 10.0);
        return stats;
    }

    // Top N videos by view count for this creator
    public List<Map<String, Object>> getTopVideosByViews(Long creatorId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<Video> videoPage = videoRepository.findByUserIdAndStatusOrderByViewCountDesc(creatorId, VideoStatus.APPROVED, pageable);
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