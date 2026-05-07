package com.final_year.v2.service;

import com.final_year.v2.model.*;
import com.final_year.v2.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EngagementService {

    @Autowired
    private WatchHistoryRepository watchHistoryRepository;
    @Autowired
    private ViewRecordRepository viewRecordRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VideoRepository videoRepository;

    private static final int MAX_WATCH_SECONDS = 600;

    @Transactional
    public void recordWatchTime(Long userId, Long videoId, int seconds) {
        if (seconds <= 0) return;
        if (seconds > MAX_WATCH_SECONDS) seconds = MAX_WATCH_SECONDS;

        User user = userRepository.findById(userId).orElseThrow();
        Video video = videoRepository.findById(videoId).orElseThrow();

        BigDecimal weight = isPaidUser(user) ? BigDecimal.ONE : new BigDecimal("0.5");

        WatchHistory wh = new WatchHistory();
        wh.setUser(user);
        wh.setVideo(video);
        wh.setWatchTimeSeconds(seconds);
        wh.setUserWeight(weight);
        wh.setCreatedAt(LocalDateTime.now());
        watchHistoryRepository.save(wh);
    }

    @Transactional
    public void recordView(Long userId, Long videoId) {
        Optional<ViewRecord> existing = viewRecordRepository.findByUserIdAndVideoId(userId, videoId);
        if (existing.isPresent()) return;

        User user = userRepository.findById(userId).orElseThrow();
        Video video = videoRepository.findById(videoId).orElseThrow();

        BigDecimal weight = isPaidUser(user) ? BigDecimal.ONE : new BigDecimal("0.5");

        ViewRecord vr = new ViewRecord();
        vr.setUser(user);
        vr.setVideo(video);
        vr.setUserWeight(weight);
        vr.setCreatedAt(LocalDateTime.now());
        viewRecordRepository.save(vr);
    }

    private boolean isPaidUser(User user) {
        return user.getSubscriptionExpiry() != null &&
                user.getSubscriptionExpiry().isAfter(LocalDateTime.now()) &&
                user.getPlan() != com.final_year.v2.constaint.Plan.FREE;
    }
}